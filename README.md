# Acme Auth Service

A Java Spring Boot authentication and authorization service for the Acme platform. This service provides secure user signup, login, token refresh, and user information endpoints with JWT-based authentication.

## Overview

`acme-auth-service` integrates with the Acme ecosystem to provide:
- **Secure Authentication**: JWT-based token authentication with refresh tokens
- **User Management**: User signup and profile retrieval
- **Consistent Error Handling**: Error responses matching the acme-contracts ErrorResponse schema
- **OpenAPI Documentation**: Comprehensive API documentation for integration with acme-ui frontend and acme-tickets-service backend
- **JWT Token Verification**: Other services can verify JWTs issued by this service

## Features

- ✅ User signup with validation
- ✅ User login with email and password
- ✅ JWT access token generation
- ✅ Refresh token mechanism for obtaining new access tokens
- ✅ Get current user endpoint (/me)
- ✅ Consistent error responses matching acme-contracts schemas
- ✅ OpenAPI/Swagger documentation
- ✅ CORS configuration for frontend integration
- ✅ H2 in-memory database for development
- ✅ PostgreSQL support for production

## Tech Stack

- **Java**: 17
- **Spring Boot**: 3.2.0
- **Spring Security**: JWT-based authentication
- **Spring Data JPA**: Database access
- **H2 Database**: Development database
- **PostgreSQL**: Production database
- **JWT (JJWT)**: Token generation and validation
- **SpringDoc OpenAPI**: API documentation
- **Maven**: Build tool

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- PostgreSQL (for production)

## Quick Start

### 1. Clone the repository

```bash
git clone https://github.com/RobertLukenbillIV/acme-auth-service.git
cd acme-auth-service
```

### 2. Build the project

```bash
mvn clean install
```

### 3. Run the application

```bash
mvn spring-boot:run
```

The service will start on `http://localhost:8080`

### 4. Access the API Documentation

Open your browser and navigate to:
```
http://localhost:8080/swagger-ui.html
```

## API Endpoints

### Authentication Endpoints

#### POST /api/auth/signup
Register a new user account.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe"
}
```

**Response:** (201 Created)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

#### POST /api/auth/login
Authenticate an existing user.

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response:** (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

#### POST /api/auth/refresh
Refresh the access token using a refresh token.

**Request Body:**
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response:** (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 86400000
}
```

#### GET /api/auth/me
Get the currently authenticated user's information.

**Headers:**
```
Authorization: Bearer <access_token>
```

**Response:** (200 OK)
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe",
  "createdAt": "2024-01-01T12:00:00.000Z",
  "updatedAt": "2024-01-01T12:00:00.000Z",
  "enabled": true
}
```

### Error Responses

All error responses follow the acme-contracts ErrorResponse schema:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "Email is required",
      "code": "NotBlank"
    }
  ],
  "timestamp": "2024-01-01T12:00:00.000Z",
  "path": "/api/auth/signup",
  "requestId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Error Codes:**
- `VALIDATION_ERROR`: Request validation failed
- `UNAUTHORIZED`: Authentication failed or invalid token
- `CONFLICT`: Resource conflict (e.g., email already exists)
- `NOT_FOUND`: Resource not found
- `INTERNAL_ERROR`: Server error

## Configuration

### Application Properties

The service can be configured via `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:h2:mem:authdb
spring.datasource.username=sa
spring.datasource.password=

# JWT Configuration
jwt.secret=your-secret-key-min-256-bits
jwt.expiration=86400000
jwt.refresh-expiration=604800000
```

### Production Configuration

For production, create `application-prod.properties`:

```properties
# PostgreSQL Database
spring.datasource.url=jdbc:postgresql://localhost:5432/acme_auth
spring.datasource.username=acme_user
spring.datasource.password=your_secure_password
spring.jpa.hibernate.ddl-auto=validate

# JWT Configuration (use strong secret)
jwt.secret=your-production-secret-key-must-be-at-least-256-bits-long
jwt.expiration=86400000
jwt.refresh-expiration=604800000

# Disable H2 console in production
spring.h2.console.enabled=false
```

Run with production profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Integration with Acme Ecosystem

### For acme-ui Frontend

The frontend can authenticate users and store tokens:

```javascript
// Login
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'user@example.com', password: 'password' })
});
const { accessToken, refreshToken } = await response.json();

// Store tokens
localStorage.setItem('accessToken', accessToken);
localStorage.setItem('refreshToken', refreshToken);

// Make authenticated requests
const userResponse = await fetch('http://localhost:8080/api/auth/me', {
  headers: { 'Authorization': `Bearer ${accessToken}` }
});
const user = await userResponse.json();
```

### For acme-tickets-service Backend

The tickets service can verify JWTs from the auth service:

1. Use the same JWT secret in both services
2. Validate tokens using the same algorithm (HS256)
3. Extract user email from token subject

Example JWT validation in Node.js:
```javascript
const jwt = require('jsonwebtoken');

function verifyToken(token) {
  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);
    return decoded.sub; // Returns email
  } catch (error) {
    throw new Error('Invalid token');
  }
}
```

## OpenAPI Specification

The service exposes an OpenAPI specification at:
- JSON format: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

This specification can be used to:
- Generate client SDKs for acme-ui
- Generate client libraries for acme-tickets-service
- Document the API for integration

## Security Features

- **Password Hashing**: BCrypt with salt
- **JWT Signing**: HS256 algorithm with secret key
- **Token Expiration**: Access tokens expire after 24 hours
- **Refresh Tokens**: Refresh tokens expire after 7 days
- **CORS**: Configured to allow frontend integration
- **Input Validation**: All requests validated with Jakarta Bean Validation
- **SQL Injection Prevention**: Prepared statements via JPA

## Development

### Running Tests

```bash
mvn test
```

### H2 Console

Access the H2 database console for development:
```
http://localhost:8080/h2-console
```

- JDBC URL: `jdbc:h2:mem:authdb`
- Username: `sa`
- Password: (empty)

### Building for Production

```bash
mvn clean package -DskipTests
java -jar target/acme-auth-service-1.0.0.jar --spring.profiles.active=prod
```

## Project Structure

```
acme-auth-service/
├── src/
│   ├── main/
│   │   ├── java/com/acme/auth/
│   │   │   ├── config/           # Configuration classes
│   │   │   │   ├── OpenAPIConfig.java
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/       # REST controllers
│   │   │   │   └── AuthController.java
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── ErrorResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RefreshTokenRequest.java
│   │   │   │   ├── SignupRequest.java
│   │   │   │   └── UserResponse.java
│   │   │   ├── entity/          # JPA entities
│   │   │   │   ├── RefreshToken.java
│   │   │   │   └── User.java
│   │   │   ├── exception/       # Exception classes
│   │   │   │   ├── EmailAlreadyExistsException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   └── TokenRefreshException.java
│   │   │   ├── repository/      # JPA repositories
│   │   │   │   ├── RefreshTokenRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/        # Security components
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   └── JwtTokenProvider.java
│   │   │   ├── service/         # Business logic
│   │   │   │   └── AuthService.java
│   │   │   └── AcmeAuthServiceApplication.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/acme/auth/
└── pom.xml
```

## Related Repositories

- **acme-contracts**: Shared schemas and types (https://github.com/RobertLukenbillIV/acme-contracts)
- **acme-ui**: React frontend components (https://github.com/RobertLukenbillIV/acme-ui)
- **acme-tickets-service**: Ticket management backend (https://github.com/RobertLukenbillIV/acme-tickets-service)

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

MIT

## Contact

For questions or support, please open an issue on GitHub.
