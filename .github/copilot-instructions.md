## Purpose

Give a concise, repository-specific guide so an AI coding agent can be productive immediately in this Spring Boot auth service.

## Big picture (what this repo is)
- Java 17 Spring Boot service providing JWT authentication + refresh tokens for the Acme platform.
- Major components: REST controllers (`src/main/java/com/acme/auth/controller`), business logic/services (`service/AuthService.java`), security (`security/*`), JPA entities/repositories (`entity/`, `repository/`), DTOs (`dto/`) and global error handling (`exception/GlobalExceptionHandler.java`).
- Tokens: `JwtTokenProvider` signs/validates JWTs (HS256 via JJWT). Refresh tokens are persisted (`RefreshToken` entity + `RefreshTokenRepository`) and the service keeps only the latest refresh token per user.

## Key files to reference (examples)
- `src/main/java/com/acme/auth/controller/AuthController.java` — public API surface (/api/auth/*).
- `src/main/java/com/acme/auth/service/AuthService.java` — signup/login/refresh flows and where tokens are created.
- `src/main/java/com/acme/auth/security/JwtTokenProvider.java` — token creation/validation and the `jwt.secret` / expiration properties.
- `src/main/java/com/acme/auth/security/JwtAuthenticationFilter.java` — extracts and authenticates JWT per request.
- `src/main/java/com/acme/auth/config/SecurityConfig.java` — security filter chain, CORS, and permitted endpoints (e.g. /api/auth/*, swagger, h2-console).
- `src/main/resources/application.properties` — runtime defaults (H2 in-memory, jwt.* defaults).
- `src/main/java/com/acme/auth/exception/GlobalExceptionHandler.java` and `src/main/java/com/acme/auth/dto/ErrorResponse.java` — project error response shape.

## Important conventions & patterns
- Endpoints live under `/api/auth` and use DTOs in `dto/` for requests/responses (follow those DTO shapes).
- Authentication: code authenticates with `AuthenticationManager#authenticate(new UsernamePasswordAuthenticationToken(email, password))`, sets the SecurityContext, then calls `JwtTokenProvider.generateToken(...)` to return access tokens.
- Refresh token lifecycle: `AuthService.createRefreshToken` deletes existing tokens for a user (`RefreshTokenRepository.deleteByUser(user)`) — follow this approach when changing refresh logic.
- Secrets: `jwt.secret` is read from `${JWT_SECRET:...}` in `application.properties`. Do not hardcode production secrets; use environment variables (JWT_SECRET) for prod runs.
- Database: H2 is the default for dev. Production expects PostgreSQL and profile `prod` (see README and `application-prod.properties` guidance).
- Lombok is used for entities (`@Data`, `@NoArgsConstructor`, etc.) — be careful when editing generated getters/setters.

## Build / run / test quick commands
- Build: `mvn clean install` (runs compilation + tests).
- Run locally: `mvn spring-boot:run` (defaults to H2). Swagger UI: `http://localhost:8080/swagger-ui.html`.
- Run with production profile: `mvn spring-boot:run -Dspring-boot.run.profiles=prod` and set `JWT_SECRET` in env.
- Tests: `mvn test`.

## What to watch for when editing
- SecurityConfig explicitly permits `/h2-console/**` and `swagger` endpoints for dev — do not accidentally lock these during small changes; update CORS origins (there's a TODO comment) when changing frontends.
- Token code relies on `JwtTokenProvider.getSigningKey()` using the `jwt.secret` string length — ensure any replacement secret is long enough for HMAC (>= 256 bits recommended).
- `RefreshTokenRepository.deleteByUser` is a `@Modifying` query; if you change it, preserve transactional semantics used in `AuthService`.
- `JwtTokenProvider.getEmailFromToken` uses JJWT parser; if upgrading JJWT or changing parsing behavior, validate both `validateToken` and `getEmailFromToken` flows with real tokens.

## Integration points / external dependencies
- Shared contract: responses follow `acme-contracts` ErrorResponse schema — see `dto/ErrorResponse.java` and README examples.
- Other services validate tokens by sharing the same `jwt.secret` (HS256). If you need to migrate to asymmetric keys, update all services accordingly.
- OpenAPI is configured in `config/OpenAPIConfig.java` and exposes JSON at `/api-docs` and UI at `/swagger-ui.html`.

## Small examples for quick edits
- Add a new protected endpoint: add controller method under `/api/auth` (or another controller), annotate for security if needed; SecurityConfig already secures non-whitelisted routes.
- When adding persistence fields to `User` or `RefreshToken`, update DTO mappers in `AuthService.mapToUserResponse` and JPA migrations (if adding prod DB migrations later).

## CI / quality checks
- Repo uses Maven; run `mvn -q -DskipTests=false test` to validate tests locally before opening a PR.
- Keep logging level changes localized to `application.properties` (currently `logging.level.com.acme.auth=DEBUG`).

## If you're unsure
- Prefer small, incremental changes and run `mvn test` locally. Look at `AuthService`, `JwtTokenProvider`, and `SecurityConfig` together to understand auth-related changes.

---
If any part of this is unclear or you want more detail (examples of token payloads, more policy on CORS/prod secrets, or unit-test patterns), tell me which area to expand and I'll iterate.
