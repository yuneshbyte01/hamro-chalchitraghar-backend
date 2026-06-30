# Maintenance Guide

## How To Add A New Feature

1. Define or update request/response DTOs under `dto/<domain>`.
2. Add or update JPA entities/enums under `model` when persistence changes are needed.
3. Add repository methods under `repository` for required queries.
4. Add business logic to a service interface and implementation under `service/<domain>`.
5. Add controller endpoints under `controller/<area>`.
6. Add mapper logic under `mapper` if responses expose entity data.
7. Add validation annotations to request DTOs.
8. Add custom exceptions or extend `GlobalExceptionHandler` when the feature introduces new failure modes.
9. Add tests for service rules and controller behavior.
10. Update docs in `docs/`.

## How To Add Or Modify APIs

- Public customer-read endpoints currently live under `controller/publicapi`.
- Auth endpoints live under `controller/auth`.
- Authenticated booking endpoints live under `controller/booking`.
- Admin endpoints live under `controller/admin`.
- Security access rules are centralized in `SecurityConfig`.
- Keep request payloads in request DTO classes and response payloads in response DTO classes.
- Keep controllers thin; put business rules in services.
- Return `ResponseEntity` with explicit status codes for create/delete operations.
- Update `docs/API.md` when endpoint behavior changes.

## How To Run Tests

Windows:

```powershell
.\mvnw.cmd test
```

Unix-like shells:

```bash
./mvnw test
```

Current tests:

- `HamroChalchitragharBackendApplicationTests.contextLoads()`

Recommended test additions:

- Auth registration, login, and token refresh tests.
- Admin authorization tests.
- Show overlap validation tests.
- Seat template and show seat generation tests.
- Booking validate/create/confirm/cancel lifecycle tests.
- Error response contract tests.

## Logging And Debugging

- `GlobalExceptionHandler` logs handled exceptions with SLF4J.
- `JwtAuthenticationFilter` logs token expiration and token validation errors.
- `spring.jpa.show-sql=true` is enabled, so SQL is printed during runtime.
- No custom logging configuration file was found.
- No correlation IDs, request logging filter, tracing, metrics, or log aggregation integration was found.

## Known Risks And Technical Debt

- Hardcoded database credentials in `application.yaml`.
- Hardcoded JWT secret in `JwtUtil`.
- `ddl-auto: update` is used instead of versioned migrations.
- No production profile or environment-variable-based configuration.
- No Dockerfile, docker-compose, or CI/CD workflow found.
- Test coverage is minimal.
- `createBooking` currently rejects non-expired `LOCKED` seats, while `validateAndLockSeats` locks seats first. This appears to conflict with the documented validate-then-create flow and needs confirmation.
- Seat lock ownership is not stored, even though `validateAndLockSeats` receives `userId`.
- Expired seat locks are not cleaned by a scheduled job.
- `releaseSeatLocks` appears to release locks that have not expired, based on `lockExpiresAt.isAfter(now)`, which may be unintended.
- Registration only creates `CUSTOMER` users; no admin/staff creation endpoint exists.
- `GET /api/shows` and admin list endpoints are unpaginated.
- Several enum values exist without complete workflow coverage in services.
- Some rules are enforced only in services and not by database constraints.
- Login for a nonexistent email can return a generic `500` because user lookup throws `RuntimeException`.
- CORS is configured in both `CORSConfig` and `SecurityConfig`, creating duplicate configuration sources.

## Routine Maintenance Checklist

- Run `.\mvnw.cmd test` before merging changes.
- Review `SecurityConfig` whenever adding routes.
- Keep DTO examples in `docs/API.md` synchronized with mapper output.
- Avoid adding secrets to source-controlled configuration.
- Add migration files if a migration tool is introduced.
- Add or update service tests for each business rule change.
