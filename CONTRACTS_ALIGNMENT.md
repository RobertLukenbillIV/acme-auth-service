# Contracts Alignment Implementation Summary

## Overview
This branch implements missing features to align the acme-auth-service with acme-contracts specifications for multi-tenant authentication with RBAC.

## ✅ Already Implemented (Before This Branch)
- REST Controller with 4 endpoints (POST /auth/signup, POST /auth/login, POST /auth/refresh, GET /auth/me)
- JWT Token generation and validation (HS256)
- Basic User entity with email, password, name
- Error handling with ErrorResponse DTOs
- OpenAPI/Swagger documentation
- Spring Security configuration

## ✅ Newly Implemented (This Branch)

### 1. Multi-Tenancy Support
**Files Created:**
- `src/main/java/com/acme/auth/entity/Tenant.java` - Tenant entity with id, name, slug, timestamps
- `src/main/java/com/acme/auth/repository/TenantRepository.java` - Repository for tenant operations

**Files Modified:**
- `src/main/java/com/acme/auth/entity/User.java` - Added:
  - `tenant_id` foreign key relationship
  - `roles` collection (ElementCollection for user_roles table)
  - Database indexes for performance

**Database Migration:**
- `src/main/resources/db/migration/V2__Add_tenants_and_roles.sql` - Creates:
  - `tenants` table
  - `user_roles` table
  - Adds `tenant_id` column to users
  - Inserts default tenant for backward compatibility

### 2. Enhanced JWT Tokens with Claims
**Files Modified:**
- `src/main/java/com/acme/auth/security/JwtTokenProvider.java` - Added:
  - `generateTokenFromUser()` method that embeds:
    - `tenant_id` claim
    - `roles[]` claim
    - `scopes[]` claim (auto-generated from roles)
    - `exp` expiration timestamp
  - Scope generation logic:
    - `ROLE_ADMIN` → tickets:read:any, tickets:write:any, tickets:delete:any, users:read:any, users:write:any
    - `ROLE_AGENT` → tickets:read:assigned, tickets:write:assigned, tickets:read:any
    - `ROLE_USER` → tickets:read:own, tickets:write:own
  - Helper methods: `getTenantIdFromToken()`, `getRolesFromToken()`, `getScopesFromToken()`

### 3. RBAC / Scope-Based Authorization
**Files Created:**
- `src/main/java/com/acme/auth/security/RequireRole.java` - Annotation for role-based access
  ```java
  @RequireRole({"ROLE_ADMIN", "ROLE_AGENT"})  // User must have one of these roles
  @RequireRole(value = {"ROLE_ADMIN"}, requireAll = true)  // Must have all roles
  ```

- `src/main/java/com/acme/auth/security/RequireScope.java` - Annotation for scope-based access
  ```java
  @RequireScope({"tickets:write:any"})  // User must have this scope
  @RequireScope(value = {"tickets:read:any", "tickets:write:any"}, requireAll = true)
  ```

- `src/main/java/com/acme/auth/security/RbacAspect.java` - AOP aspect that:
  - Intercepts methods annotated with @RequireRole or @RequireScope
  - Extracts JWT token from Authorization header
  - Validates user has required roles/scopes
  - Throws AccessDeniedException if unauthorized

**Dependencies Added:**
- `pom.xml` - Added spring-boot-starter-aop for AspectJ support

### 4. application/problem+json Error Format
**Files Modified:**
- `src/main/java/com/acme/auth/exception/GlobalExceptionHandler.java` - Updated:
  - All error responses now use `Content-Type: application/problem+json`
  - Added `AccessDeniedException` handler for RBAC violations (403 Forbidden)
  - Consistent error response format across all exceptions

## 🔄 Still Requires Integration Work

The following components are implemented but need integration:

### AuthService Updates Needed
- Update `signup()` method to:
  - Accept or auto-assign tenant
  - Assign default role (e.g., ROLE_USER)
  - Use `generateTokenFromUser()` instead of `generateToken()`
  
- Update `login()` method to:
  - Use `generateTokenFromUser()` to include tenant/roles/scopes in token

- Update `refreshToken()` method to:
  - Use `generateTokenFromUser()` for new access token

### SignupRequest DTO Enhancement
- Consider adding optional `tenantSlug` field
- Consider adding optional `roles` field (for admin user creation)

### UserResponse DTO Enhancement
- Add `tenantId` field
- Add `roles[]` field to match AuthUser schema from acme-contracts

## 📋 Usage Examples

### Creating Users with Roles (After AuthService Integration)
```java
// Signup creates user with default role
User user = new User(email, encodedPassword, name, tenant, List.of("ROLE_USER"));

// Admin can create users with specific roles
User agent = new User(email, encodedPassword, name, tenant, List.of("ROLE_AGENT"));
User admin = new User(email, encodedPassword, name, tenant, List.of("ROLE_ADMIN", "ROLE_AGENT"));
```

### Protecting Endpoints with RBAC
```java
@GetMapping("/admin/users")
@RequireRole("ROLE_ADMIN")
public List<User> getAllUsers() {
    // Only admins can access
}

@GetMapping("/tickets")
@RequireScope({"tickets:read:any", "tickets:read:own"})
public List<Ticket> getTickets() {
    // Users with either scope can access
}

@DeleteMapping("/tickets/{id}")
@RequireScope(value = "tickets:delete:any", requireAll = true)
public void deleteTicket(@PathVariable String id) {
    // Only users with delete permission
}
```

### JWT Token Structure (After Integration)
```json
{
  "sub": "user@example.com",
  "tenant_id": "550e8400-e29b-41d4-a716-446655440000",
  "roles": ["ROLE_USER"],
  "scopes": ["tickets:read:own", "tickets:write:own"],
  "iat": 1699200000,
  "exp": 1699286400
}
```

## 🧪 Testing Recommendations

1. **Tenant Isolation**: Verify users can only access resources within their tenant
2. **Role Hierarchy**: Test that ROLE_ADMIN has all permissions
3. **Scope Validation**: Ensure endpoints properly reject requests without required scopes
4. **Token Claims**: Verify JWT tokens contain correct tenant_id, roles, and scopes
5. **Multi-Tenancy**: Create users in different tenants and verify isolation

## 📝 Next Steps

1. Update AuthService to use new tenant and role features
2. Update DTOs to include tenant and role information
3. Add tenant context to security context holder
4. Create admin endpoints for tenant/user management
5. Add integration tests for RBAC
6. Document API with tenant and role requirements

## 🔒 Security Considerations

- Default tenant is created for backward compatibility (ID: 00000000-0000-0000-0000-000000000001)
- Roles are stored in separate table for flexibility
- Scopes are derived from roles (not stored) for consistency
- JWT tokens are signed with HS256 (configurable via jwt.secret)
- Access control enforced at method level via AOP

## 🎯 Alignment with acme-contracts

| Requirement | Status | Notes |
|-------------|--------|-------|
| User entity with tenantId | ✅ Implemented | Foreign key to tenants table |
| User entity with roles | ✅ Implemented | ElementCollection in user_roles table |
| JWT with tenant_id claim | ✅ Implemented | In JwtTokenProvider |
| JWT with roles[] claim | ✅ Implemented | In JwtTokenProvider |
| JWT with scopes[] claim | ✅ Implemented | Auto-generated from roles |
| RBAC enforcement | ✅ Implemented | @RequireRole and @RequireScope annotations |
| application/problem+json | ✅ Implemented | All error responses |
| OpenAPI documentation | ✅ Already existed | SpringDoc configuration |
