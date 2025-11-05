# Integration Guide

This guide explains how to integrate the acme-auth-service with other components of the Acme ecosystem.

## Table of Contents

1. [Frontend Integration (acme-ui)](#frontend-integration-acme-ui)
2. [Backend Integration (acme-tickets-service)](#backend-integration-acme-tickets-service)
3. [JWT Token Structure](#jwt-token-structure)
4. [Error Handling](#error-handling)
5. [Production Deployment](#production-deployment)

## Frontend Integration (acme-ui)

### Authentication Flow

```javascript
// 1. User Signup
async function signup(email, password, name) {
  const response = await fetch('http://localhost:8080/api/auth/signup', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password, name })
  });
  
  if (response.ok) {
    const { accessToken, refreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    return true;
  }
  
  const error = await response.json();
  throw new Error(error.message);
}

// 2. User Login
async function login(email, password) {
  const response = await fetch('http://localhost:8080/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  if (response.ok) {
    const { accessToken, refreshToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    localStorage.setItem('refreshToken', refreshToken);
    return true;
  }
  
  throw new Error('Invalid credentials');
}

// 3. Get Current User
async function getCurrentUser() {
  const token = localStorage.getItem('accessToken');
  
  const response = await fetch('http://localhost:8080/api/auth/me', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  
  if (response.ok) {
    return await response.json();
  }
  
  if (response.status === 401) {
    // Token expired, try to refresh
    await refreshAccessToken();
    return getCurrentUser(); // Retry
  }
  
  throw new Error('Failed to get user');
}

// 4. Refresh Access Token
async function refreshAccessToken() {
  const refreshToken = localStorage.getItem('refreshToken');
  
  const response = await fetch('http://localhost:8080/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  
  if (response.ok) {
    const { accessToken } = await response.json();
    localStorage.setItem('accessToken', accessToken);
    return accessToken;
  }
  
  // Refresh token expired, redirect to login
  localStorage.clear();
  window.location.href = '/login';
  throw new Error('Session expired');
}

// 5. Logout
function logout() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  window.location.href = '/login';
}
```

### React Integration Example

```jsx
import React, { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext();

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadUser();
  }, []);

  async function loadUser() {
    const token = localStorage.getItem('accessToken');
    if (!token) {
      setLoading(false);
      return;
    }

    try {
      const response = await fetch('http://localhost:8080/api/auth/me', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      
      if (response.ok) {
        const userData = await response.json();
        setUser(userData);
      }
    } catch (error) {
      console.error('Failed to load user:', error);
    } finally {
      setLoading(false);
    }
  }

  async function login(email, password) {
    const response = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password })
    });

    if (response.ok) {
      const { accessToken, refreshToken } = await response.json();
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      await loadUser();
      return true;
    }
    
    return false;
  }

  function logout() {
    localStorage.clear();
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  return useContext(AuthContext);
}
```

## Backend Integration (acme-tickets-service)

### Node.js/Express JWT Verification

```javascript
const jwt = require('jsonwebtoken');

// Middleware to verify JWT tokens from acme-auth-service
function authenticateToken(req, res, next) {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1]; // Bearer TOKEN

  if (!token) {
    return res.status(401).json({
      code: 'UNAUTHORIZED',
      message: 'Access token is required',
      timestamp: new Date().toISOString()
    });
  }

  try {
    // Use the same JWT secret as acme-auth-service
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    
    // The token subject contains the user's email
    req.user = {
      email: decoded.sub,
      iat: decoded.iat,
      exp: decoded.exp
    };
    
    next();
  } catch (error) {
    if (error.name === 'TokenExpiredError') {
      return res.status(401).json({
        code: 'UNAUTHORIZED',
        message: 'Access token has expired',
        timestamp: new Date().toISOString()
      });
    }
    
    return res.status(401).json({
      code: 'UNAUTHORIZED',
      message: 'Invalid access token',
      timestamp: new Date().toISOString()
    });
  }
}

// Example usage in routes
app.get('/api/tickets', authenticateToken, async (req, res) => {
  // req.user.email contains the authenticated user's email
  const userEmail = req.user.email;
  
  // Fetch tickets for this user
  const tickets = await getTicketsForUser(userEmail);
  res.json(tickets);
});
```

### Environment Configuration

Both services must share the same JWT secret. In production:

**acme-auth-service** (application.properties):
```properties
jwt.secret=${JWT_SECRET}
```

**acme-tickets-service** (.env):
```env
JWT_SECRET=your-shared-secret-key-min-256-bits
```

### Java/Spring Boot JWT Verification

If acme-tickets-service is also Java/Spring Boot, you can use the same JWT libraries:

```java
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidator {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
    
    public String validateTokenAndGetEmail(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            return claims.getSubject(); // Returns email
        } catch (ExpiredJwtException e) {
            throw new RuntimeException("Token expired");
        } catch (JwtException e) {
            throw new RuntimeException("Invalid token");
        }
    }
}
```

## JWT Token Structure

### Access Token

The JWT access tokens issued by acme-auth-service have the following structure:

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user@example.com",
    "iat": 1704067200,
    "exp": 1704153600
  }
}
```

- `sub`: Subject (user's email address)
- `iat`: Issued at timestamp
- `exp`: Expiration timestamp (24 hours from issuance)

### Refresh Token

Refresh tokens are UUIDs stored in the database with an expiration of 7 days. They are not JWTs.

## Error Handling

All error responses follow the acme-contracts ErrorResponse schema:

```typescript
interface ErrorResponse {
  code: string;           // Error code enum
  message: string;        // Human-readable error message
  details?: Array<{       // Validation error details (optional)
    field: string;
    message: string;
    code?: string;
  }>;
  timestamp: string;      // ISO 8601 datetime
  path?: string;          // Request path
  requestId?: string;     // UUID for tracking
}
```

### Error Codes

- `VALIDATION_ERROR`: Request validation failed
- `UNAUTHORIZED`: Authentication failed or invalid token
- `CONFLICT`: Resource conflict (e.g., email already exists)
- `NOT_FOUND`: Resource not found
- `INTERNAL_ERROR`: Server error

### Example Error Handling

```javascript
async function handleApiError(response) {
  const error = await response.json();
  
  switch (error.code) {
    case 'VALIDATION_ERROR':
      // Show validation errors to user
      error.details.forEach(detail => {
        console.error(`${detail.field}: ${detail.message}`);
      });
      break;
      
    case 'UNAUTHORIZED':
      // Try to refresh token or redirect to login
      if (error.message.includes('expired')) {
        await refreshAccessToken();
      } else {
        logout();
      }
      break;
      
    case 'CONFLICT':
      // Handle conflicts (e.g., email already exists)
      console.error(error.message);
      break;
      
    default:
      console.error('Unexpected error:', error.message);
  }
}
```

## Production Deployment

### Environment Variables

Set these environment variables in production:

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://db-host:5432/acme_auth
SPRING_DATASOURCE_USERNAME=acme_user
SPRING_DATASOURCE_PASSWORD=secure_password

# JWT (must match across all services)
JWT_SECRET=your-production-secret-key-must-be-at-least-256-bits-long

# Server
SERVER_PORT=8080

# CORS (update with actual frontend URL)
ALLOWED_ORIGINS=https://acme-ui.example.com
```

### CORS Configuration

Update `SecurityConfig.java` for production:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(
        System.getenv("ALLOWED_ORIGINS") != null 
            ? System.getenv("ALLOWED_ORIGINS") 
            : "http://localhost:3000"
    ));
    // ... rest of configuration
}
```

### Database Migration

For production, use proper database migrations:

1. Set `spring.jpa.hibernate.ddl-auto=validate` in production
2. Use Flyway or Liquibase for schema management
3. Run migrations before deploying new versions

### Health Checks

The service includes Spring Boot Actuator health endpoints:

```bash
# Health check endpoint
GET /actuator/health

# Response
{
  "status": "UP"
}
```

### Monitoring

Monitor these metrics:
- Authentication success/failure rates
- Token refresh rates
- Response times
- Database connection pool status
- JVM memory usage

## OpenAPI Integration

### Generating Client SDKs

Use the OpenAPI specification to generate client libraries:

```bash
# Get the OpenAPI spec
curl http://localhost:8080/api-docs > openapi.json

# Generate TypeScript client for acme-ui
openapi-generator-cli generate \
  -i openapi.json \
  -g typescript-axios \
  -o ./acme-ui/src/api/auth-client

# Generate Java client for acme-tickets-service
openapi-generator-cli generate \
  -i openapi.json \
  -g java \
  -o ./acme-tickets-service/auth-client
```

### Using Generated Client

```typescript
// TypeScript/React
import { AuthApi, Configuration } from './api/auth-client';

const authApi = new AuthApi(new Configuration({
  basePath: 'http://localhost:8080'
}));

// Signup
const response = await authApi.signup({
  email: 'user@example.com',
  password: 'SecurePass123!',
  name: 'John Doe'
});

// Login
const loginResponse = await authApi.login({
  email: 'user@example.com',
  password: 'SecurePass123!'
});
```

## Support

For issues or questions:
- GitHub Issues: https://github.com/RobertLukenbillIV/acme-auth-service/issues
- Documentation: See README.md
- API Docs: http://localhost:8080/swagger-ui.html
