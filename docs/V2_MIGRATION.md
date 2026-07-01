# V2 Migration Notes

## Version History

- V1 is preserved in the `v1` branch.
- V1 is tagged as `v1.0.0`.
- V2 work continues on the `v2` branch.

## Major Changes In V2

### Modular Monolith

V1 used a flatter layered package structure.

V2 organizes code into:

- `modules/auth`
- `modules/users`
- `modules/movies`
- `modules/halls`
- `modules/shows`
- `modules/seats`
- `modules/bookings`
- `applications/auth`
- `applications/publicapi`
- `applications/customer`
- `applications/staff`
- `applications/admin`
- `shared/config`
- `shared/security`
- `shared/exception`
- `shared/response`

### Role-Based Endpoint Groups

V2 moved mixed routes into explicit role-based groups:

- Auth: `/api/auth/**`
- Public: `/api/public/**`
- Customer: `/api/customer/**`
- Staff: `/api/staff/**`
- Admin: `/api/admin/**`

Examples:

- Public movies moved to `/api/public/movies`.
- Public health moved to `/api/public/health`.
- Customer bookings moved to `/api/customer/bookings`.

### PostgreSQL Migration

V2 uses PostgreSQL instead of MySQL.

Configuration is environment-driven through:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

### Flyway Schema Management

V2 uses Flyway migrations under:

```text
src/main/resources/db/migration
```

Hibernate now validates the schema:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

This replaces the old schema-update approach.

### Environment-Based Configuration

V2 moved database, JWT, and CORS values to environment variables.

Profiles:

- `dev`
- `prod`
- `test`

`.env.example` documents the expected local variables.

### Booking Seat Hold Flow

V2 replaced the old validation-first booking flow with a seat hold flow:

1. Hold seats with `POST /api/customer/bookings/hold`.
2. Create booking from available seats or seats held by the same user.
3. Confirm booking to mark seats `BOOKED`.
4. Cancel initiated booking to release seats.

Seat locks now track ownership with:

```text
locked_by_user_id
```

### Standard API Response

V2 standardizes success and error responses through `ApiResponse<T>`:

```json
{
  "success": true,
  "message": "Success message",
  "data": {},
  "errors": []
}
```

Errors use the same fields with `success: false` and `data: null`.

### Integration Tests

V2 added MockMvc integration tests for:

- Auth
- Public APIs
- Admin APIs
- Authorization
- Standard response shape
- Show validation
- Seat layout generation
- Booking hold/create/confirm/cancel flow

Tests use the `test` profile with H2 in PostgreSQL compatibility mode and do not require a local PostgreSQL database.

## Partial Or Planned Items

Implemented staff endpoint:

- `GET /api/staff/bookings/{bookingId}`

Planned:

- Staff check-in endpoint.
- Staff operational show/seat endpoints.
- Admin staff creation endpoint.
- Docker and CI/CD.
- Payment, email, and SMS integrations.

## Phase 10 Docker And CI

V2 added:

- Multi-stage `Dockerfile`.
- `.dockerignore`.
- `docker-compose.yml` with PostgreSQL and app services.
- GitHub Actions workflow at `.github/workflows/ci.yml`.

CI runs:

```bash
./mvnw -B clean test
```

with `SPRING_PROFILES_ACTIVE=test`.
