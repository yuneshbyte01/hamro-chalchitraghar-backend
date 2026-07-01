# Hamro Chalchitraghar Backend V2 Project Report

## Summary

Hamro Chalchitraghar backend V2 is a Java 21 Spring Boot 3.x cinema booking API. It uses a modular monolith structure, PostgreSQL, Flyway migrations, JWT authentication, role-based endpoint groups, a seat hold booking flow, standard API responses, and MockMvc integration tests.

## Current Stack

- Java 21
- Spring Boot 3.5.9
- Spring Web
- Spring Security with JWT
- Spring Data JPA and Hibernate
- PostgreSQL
- Flyway
- Maven wrapper
- JUnit, MockMvc, Spring Security Test
- H2 for test profile only

## Main Implemented Areas

- Auth registration, login, and token refresh.
- Public movie, hall, show, seat, and health endpoints.
- Customer profile and booking endpoints.
- Customer seat hold flow.
- Staff booking detail lookup.
- Admin movie, hall, show, seat layout, and user management.
- Standard `ApiResponse<T>` success and error format.
- Centralized exception handling.
- Integration tests for core API behavior and authorization.

## Endpoint Groups

| Group | Base Path | Access |
| --- | --- | --- |
| Auth | `/api/auth/**` | Public |
| Public | `/api/public/**` | Public |
| Customer | `/api/customer/**` | CUSTOMER, STAFF, ADMIN |
| Staff | `/api/staff/**` | STAFF, ADMIN |
| Admin | `/api/admin/**` | ADMIN |

## Important Endpoints

Auth:

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

Public:

- `GET /api/public/health`
- `GET /api/public/movies`
- `GET /api/public/movies/{id}`
- `GET /api/public/movies/now-showing`
- `GET /api/public/movies/upcoming`
- `GET /api/public/halls`
- `GET /api/public/halls/{id}`
- `GET /api/public/halls/active`
- `GET /api/public/shows`
- `GET /api/public/shows/{id}`
- `GET /api/public/shows/movie/{movieId}`
- `GET /api/public/shows?movieId={movieId}&date={date}`
- `GET /api/public/shows/{showId}/seats`

Customer:

- `GET /api/customer/profile`
- `POST /api/customer/bookings/hold`
- `POST /api/customer/bookings`
- `POST /api/customer/bookings/{bookingId}/confirm`
- `GET /api/customer/bookings/my`
- `GET /api/customer/bookings/{bookingId}`
- `POST /api/customer/bookings/{bookingId}/cancel`

Staff:

- `GET /api/staff/bookings/{bookingId}`

Admin:

- `GET /api/admin/movies`
- `POST /api/admin/movies`
- `PUT /api/admin/movies/{id}`
- `DELETE /api/admin/movies/{id}`
- `GET /api/admin/halls`
- `POST /api/admin/halls`
- `POST /api/admin/halls/{hallId}/seat-layout`
- `GET /api/admin/shows`
- `POST /api/admin/shows`
- `PUT /api/admin/shows/{id}`
- `DELETE /api/admin/shows/{id}`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`

## Standard API Response

Success:

```json
{
  "success": true,
  "message": "Success message",
  "data": {},
  "errors": []
}
```

Error:

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": []
}
```

## Database

Database engine: PostgreSQL.

Schema management:

- Flyway migrations under `src/main/resources/db/migration`.
- Hibernate `ddl-auto: validate`.

Tables:

- `users`
- `movies`
- `halls`
- `seat_templates`
- `shows`
- `seats`
- `bookings`
- `booking_seats`

Seat holds use:

- `locked_at`
- `lock_expires_at`
- `locked_by_user_id`

## Testing

Run:

```powershell
.\mvnw.cmd test
```

Test profile:

- `src/test/resources/application-test.yaml`
- H2 in PostgreSQL compatibility mode
- Flyway disabled
- Hibernate `create-drop`

Current integration tests cover auth, public APIs, admin APIs, booking flow, authorization, and error response behavior.

## Partial Or Planned Items

- Staff booking detail is implemented.
- Staff check-in is planned.
- Admin staff creation is planned.
- Docker and CI/CD are planned for a later phase.
- Payment, email, and SMS integrations are not implemented.
