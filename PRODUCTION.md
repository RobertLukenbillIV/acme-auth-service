# Production Deployment Guide

This guide covers the production deployment requirements for the acme-auth-service.

## Prerequisites

### Required Environment Variables

```bash
# Database Configuration
DATABASE_URL=jdbc:postgresql://your-postgres-host:5432/acme_auth
DATABASE_USERNAME=acme_user
DATABASE_PASSWORD=<strong-password>

# JWT Configuration (CRITICAL - Must be at least 256 bits / 32 characters)
JWT_SECRET=<generate-strong-random-secret-min-32-chars>
JWT_EXPIRATION=3600000  # 1 hour in milliseconds
JWT_REFRESH_EXPIRATION=604800000  # 7 days in milliseconds

# CORS Configuration
CORS_ALLOWED_ORIGINS=https://app.acme.com,https://admin.acme.com

# SSL/TLS Configuration (if not using reverse proxy)
SSL_ENABLED=true
SSL_KEYSTORE_PATH=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=<keystore-password>
SSL_KEYSTORE_TYPE=PKCS12
```

### Database Setup

1. **Install PostgreSQL** (version 14 or higher recommended)

2. **Create Database and User**:
```sql
CREATE DATABASE acme_auth;
CREATE USER acme_user WITH ENCRYPTED PASSWORD 'your-password';
GRANT ALL PRIVILEGES ON DATABASE acme_auth TO acme_user;
```

3. **Run Flyway Migrations**:
Migrations will run automatically on application startup when `spring.flyway.enabled=true`

### Generate Strong JWT Secret

Use one of these methods to generate a secure JWT secret:

```bash
# Option 1: Using openssl (recommended)
openssl rand -base64 32

# Option 2: Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"

# Option 3: Using Node.js
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

## Deployment Steps

### 1. Build the Application

```bash
mvn clean package -DskipTests
```

### 2. Set Environment Variables

Create a `.env` file or set environment variables in your deployment platform:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DATABASE_URL=jdbc:postgresql://localhost:5432/acme_auth
export DATABASE_USERNAME=acme_user
export DATABASE_PASSWORD=<your-db-password>
export JWT_SECRET=<your-generated-secret>
export CORS_ALLOWED_ORIGINS=https://your-frontend.com
```

### 3. Run the Application

```bash
java -jar target/acme-auth-service-1.0.0.jar --spring.profiles.active=prod
```

## Security Checklist

- [ ] **JWT Secret**: Generated strong random secret (minimum 32 characters)
- [ ] **Database Password**: Using strong, unique password
- [ ] **CORS Origins**: Configured with specific frontend URLs (no wildcards)
- [ ] **HTTPS**: Enabled for all endpoints
- [ ] **Rate Limiting**: Configured and tested (default: 5 requests/minute per IP)
- [ ] **Database Migrations**: Tested in staging environment
- [ ] **Monitoring**: Metrics endpoint secured (/actuator/metrics requires ADMIN role)
- [ ] **H2 Console**: Disabled in production (automatic with prod profile)

## HTTPS/SSL Configuration

### Option 1: Application-Level SSL

Set these environment variables:
```bash
SSL_ENABLED=true
SSL_KEYSTORE_PATH=/path/to/keystore.p12
SSL_KEYSTORE_PASSWORD=<password>
```

Generate keystore:
```bash
keytool -genkeypair -alias acme-auth -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore keystore.p12 -validity 3650
```

### Option 2: Reverse Proxy (Recommended)

Use nginx or similar reverse proxy to handle SSL/TLS termination:

```nginx
server {
    listen 443 ssl http2;
    server_name api.acme.com;

    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;

    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring and Alerting

### Available Metrics

- **Health Check**: `GET /actuator/health` (public)
- **Application Info**: `GET /actuator/info` (public)
- **Prometheus Metrics**: `GET /actuator/prometheus` (requires ADMIN role)
- **Custom Metrics**:
  - `auth.login.success` - Successful logins
  - `auth.login.failure` - Failed login attempts

### Prometheus Configuration

Add to `prometheus.yml`:
```yaml
scrape_configs:
  - job_name: 'acme-auth-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
    basic_auth:
      username: 'admin'
      password: 'admin-password'
```

### Alert Rules

Example Prometheus alert for authentication failures:
```yaml
groups:
  - name: auth_alerts
    rules:
      - alert: HighAuthenticationFailureRate
        expr: rate(auth_login_failure_total[5m]) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate of authentication failures"
          description: "More than 10 failed authentication attempts per minute"
```

## Rate Limiting Configuration

Default rate limit: **5 requests per minute per IP address** for:
- `/api/auth/login`
- `/api/auth/signup`

To customize, modify `RateLimitingFilter.java`:
```java
// Change this line in createNewBucket() method:
Bandwidth limit = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
```

## Database Migrations

Flyway migrations are located in `src/main/resources/db/migration/`

### Creating New Migrations

1. Create file with naming pattern: `V{version}__{description}.sql`
   Example: `V2__Add_email_verification.sql`

2. Place in `src/main/resources/db/migration/`

3. Restart application - migration runs automatically

### Manual Migration Management

```bash
# Validate migrations
mvn flyway:validate

# Show migration status
mvn flyway:info

# Repair migration checksums
mvn flyway:repair
```

## Troubleshooting

### Common Issues

1. **JWT Secret Too Short**
   - Error: `The specified key byte array is X bits which is not secure enough`
   - Solution: Use at least 256-bit (32 character) secret

2. **Database Connection Refused**
   - Check PostgreSQL is running
   - Verify DATABASE_URL, username, and password
   - Ensure PostgreSQL allows connections from application host

3. **CORS Errors in Production**
   - Verify CORS_ALLOWED_ORIGINS includes your frontend URL
   - Check protocol (http vs https)
   - Verify no trailing slashes in URLs

4. **Rate Limit Too Aggressive**
   - Temporarily increase limit in `RateLimitingFilter`
   - Consider implementing user-based rate limiting

## Future Enhancements

The following features are planned for future releases:

- [ ] Email verification for new signups
- [ ] Password reset functionality via email
- [ ] Multi-factor authentication (MFA/2FA)
- [ ] OAuth2/OpenID Connect integration
- [ ] User session management and device tracking
- [ ] IP-based anomaly detection

## Support

For issues or questions:
- Create an issue on GitHub
- Check existing documentation in `/docs`
- Review logs in `/var/log/acme-auth-service/`
