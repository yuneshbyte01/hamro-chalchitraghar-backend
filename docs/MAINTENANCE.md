# Maintenance Guide

## How To Add A New Feature

1. Decide the application boundary:
   - public: `applications/publicapi`
   - customer: `applications/customer`
   - staff: `applications/staff`
   - admin: `applications/admin`
2. Add or update request/response DTOs in the relevant `modules/<domain>/dto` package.
3. Add or update entities and enums in the relevant `modules/<domain>` package if persistence changes are needed.
4. Add repository methods under `modules/<domain>/repository`.
5. Add business logic to service interfaces and implementations under `modules/<domain>/service`.
6. Add mapper logic when response DTOs expose entity data.
7. Add controller endpoints under `applications/<area>`.
8. Add validation annotations to request DTOs.
9. Add custom exceptions or extend `GlobalExceptionHandler` for new failure modes.
10. Return success responses using `ApiResponse`.
11. Add or update integration tests.
12. Update docs in `docs/`.

## How To Add Or Modify APIs

- Keep endpoint groups role-based:
  - `/api/auth/**`
  - `/api/public/**`
  - `/api/customer/**`
  - `/api/staff/**`
  - `/api/admin/**`
- Keep request payloads in request DTO classes.
- Keep response payloads in response DTO classes.
- Do not expose entities directly from controllers.
- Keep controllers thin and put business rules in services.
- Preserve status codes:
  - `200` for reads and updates.
  - `201` for creates.
  - `204` for current delete endpoints with no body.
- Update `docs/API.md` when endpoint behavior changes.
- Update `SecurityConfig` when adding a new route group.

## Standard Response Maintenance

Use `ApiResponse<T>` for non-empty responses:

```json
{
  "success": true,
  "message": "Success message",
  "data": {},
  "errors": []
}
```

Errors should use:

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": []
}
```

Validation errors should use:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": ["field: message"]
}
```

## How To Run Tests

Windows:

```powershell
.\mvnw.cmd test
```

Run compile only:

```powershell
.\mvnw.cmd clean compile
```

Run one test class:

```powershell
.\mvnw.cmd -Dtest=BookingApiIntegrationTest test
```

Current test profile:

- `src/test/resources/application-test.yaml`
- H2 in PostgreSQL compatibility mode
- Flyway disabled
- Hibernate `create-drop`

Current coverage areas:

- Auth registration, login, duplicate email, invalid login, password hashing.
- Public movie, hall, show, seat, and health responses.
- Admin movie, hall, show, and user authorization behavior.
- Show overlap and status validation.
- Seat layout generation.
- Booking hold, create, confirm, cancel, expired locks, duplicate seat IDs.
- Authorization for public, customer, staff, and admin route groups.
- Standard validation, not found, invalid JWT, and expired JWT responses.

Recommended coverage for future changes:

- Add tests for every new endpoint.
- Add authorization tests whenever `SecurityConfig` changes.
- Add migration-focused checks when schema changes become complex.
- Add service-level tests for complicated business rules if integration tests become too large.

## How To Add A Migration

1. Add a new SQL file under:

```text
src/main/resources/db/migration
```

2. Use the next version number:

```text
V10__description.sql
```

3. Keep SQL PostgreSQL-compatible.
4. Run:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

5. Start the app locally and confirm Flyway applies the migration.

Do not change old migrations that may already have run in another environment.

## Logging And Debugging

- `GlobalExceptionHandler` logs handled exceptions with SLF4J.
- `JwtAuthenticationFilter` logs token expiration and token validation errors.
- SQL logging is currently enabled with `spring.jpa.show-sql=true`.
- No custom tracing, request correlation IDs, or metrics integration is implemented.

## Known Partial Or Planned Features

- Staff booking detail is implemented: `GET /api/staff/bookings/{bookingId}`.
- Staff check-in is planned, not implemented.
- Staff booking list and staff show operations are planned, not implemented.
- Admin staff creation is planned, not implemented.
- Registration only creates `CUSTOMER` users.
- Admin and staff users currently need trusted database setup or another operational process.
- Payment, email, SMS, Docker, and CI/CD are not implemented.
- API list endpoints are not paginated.
- The database does not enforce a unique active booking per seat; service logic prevents active duplicates.

## Routine Maintenance Checklist

- Run `.\mvnw.cmd clean compile`.
- Run `.\mvnw.cmd test`.
- Review `SecurityConfig` for route changes.
- Keep `docs/API.md` synchronized with controller paths and DTO output.
- Keep `docs/DATABASE.md` synchronized with migrations.
- Avoid committing real secrets.
- Add migration files for schema changes.
- Add integration tests for new user-facing behavior.
