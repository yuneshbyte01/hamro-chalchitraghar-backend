# Hamro Chalachitraghar V2 TODO

## Goal

Upgrade the beginner V1 project into a cleaner, production-style Spring Boot backend using modular monolith architecture, PostgreSQL, Flyway migrations, better endpoint separation, improved booking flow, testing, and deployment readiness.

---

## Phase 0: Git Setup

* [x] Create `v1` branch from completed beginner project.
* [x] Push `v1` branch.
* [x] Create `v1.0.0` tag.
* [x] Push `v1.0.0` tag.
* [x] Create `v2` branch.
* [x] Push `v2` branch.
* [x] Delete old `feature/bookings` branch.
* [x] Create V2 starting checkpoint commit.

Command:

```powershell
git commit --allow-empty -m "chore: start v2 refactor"
git push
```

---

## Phase 1: Modular Monolith Structure

### Target package structure

```text
src/main/java/com/chalchitraghar
  modules
    auth
    users
    movies
    halls
    shows
    seats
    bookings

  applications
    publicapi
    customer
    staff
    admin

  shared
    config
    security
    exception
    response
```

### Tasks

* [x] Move `config` package to `shared/config`.
* [x] Move `security` package to `shared/security`.
* [x] Move `exception` package to `shared/exception`.
* [x] Move auth-related controller, service, DTOs to `modules/auth`.
* [x] Move user-related model, repository, service, DTOs, mapper to `modules/users`.
* [x] Move movie-related model, repository, service, DTOs, mapper to `modules/movies`.
* [x] Move hall-related model, repository, service, DTOs, mapper to `modules/halls`.
* [x] Move show-related model, repository, service, DTOs, mapper to `modules/shows`.
* [x] Move seat-related model, repository, service, DTOs, mapper to `modules/seats`.
* [x] Move booking-related model, repository, service, DTOs, mapper to `modules/bookings`.
* [x] Fix package imports after moving files.
* [x] Run compile check.

Command:

```powershell
.\mvnw.cmd clean compile
```

* [x] Commit modular structure.

Command:

```powershell
git add .
git commit -m "refactor: restructure project into modular monolith"
git push
```

---

## Phase 2: Role-Based Endpoint Separation

### Target endpoint groups

```text
/api/public/**
/api/customer/**
/api/staff/**
/api/admin/**
/api/auth/**
```

### Public APIs

* [x] Move public movie browsing to `/api/public/movies`.
* [x] Move public hall browsing to `/api/public/halls`.
* [x] Move public show browsing to `/api/public/shows`.
* [x] Move public seat availability to `/api/public/shows/{showId}/seats`.

### Customer APIs

* [x] Add `/api/customer/profile`.
* [x] Add `/api/customer/bookings`.
* [x] Add `/api/customer/bookings/hold`.
* [x] Add `/api/customer/bookings`.
* [x] Add `/api/customer/bookings/{id}`.
* [x] Add `/api/customer/bookings/{id}/confirm`.
* [x] Add `/api/customer/bookings/{id}/cancel`.

### Staff APIs

* [ ] Add `/api/staff/shows/today`.
* [ ] Add `/api/staff/shows/{id}/seats`.
* [ ] Add `/api/staff/bookings`.
* [x] Add `/api/staff/bookings/{id}`.
* [ ] Add `/api/staff/bookings/{id}/check-in`.

### Admin APIs

* [x] Keep admin movie management under `/api/admin/movies`.
* [x] Keep admin hall management under `/api/admin/halls`.
* [x] Keep admin show management under `/api/admin/shows`.
* [x] Improve admin user management under `/api/admin/users`.
* [ ] Add admin staff creation endpoint.
* [ ] Add admin booking management endpoints.

### Security rules

* [x] Permit `/api/auth/**`.
* [x] Permit `/api/public/**`.
* [x] Restrict `/api/customer/**` to `CUSTOMER`, `STAFF`, and `ADMIN`.
* [x] Restrict `/api/staff/**` to `STAFF` and `ADMIN`.
* [x] Restrict `/api/admin/**` to `ADMIN`.

---

## Phase 3: PostgreSQL Migration

* [x] Replace MySQL dependency with PostgreSQL driver.
* [x] Update `application.yaml` datasource config.
* [x] Create PostgreSQL database.
* [x] Test app connection with PostgreSQL.
* [x] Remove MySQL-specific configuration.
* [x] Commit PostgreSQL migration.

---

## Phase 4: Flyway Migrations

* [x] Add Flyway dependency.
* [x] Change Hibernate from `ddl-auto: update` to `ddl-auto: validate`.
* [ ] Create migration folder:

```text
src/main/resources/db/migration
```

* [x] Add `V1__create_users_table.sql`.
* [x] Add `V2__create_movies_table.sql`.
* [x] Add `V3__create_halls_table.sql`.
* [x] Add `V4__create_seat_templates_table.sql`.
* [x] Add `V5__create_shows_table.sql`.
* [x] Add `V6__create_seats_table.sql`.
* [x] Add `V7__create_bookings_table.sql`.
* [x] Add `V8__create_booking_seats_table.sql`.
* [x] Run migration.
* [x] Commit Flyway setup.

---

## Phase 5: Environment-Based Configuration

* [x] Remove hardcoded database username.
* [x] Remove hardcoded database password.
* [x] Remove hardcoded JWT secret.
* [x] Remove hardcoded CORS origin.
* [x] Add environment variables.
* [x] Add `.env.example`.
* [x] Add `application-dev.yaml`.
* [x] Add `application-prod.yaml`.
* [x] Commit configuration cleanup.

---

## Phase 6: Booking Flow Redesign

### Current problem

The old flow locks seats during validation, but booking creation rejects locked seats.

### Target flow

```text
POST /api/customer/bookings/hold
POST /api/customer/bookings
POST /api/customer/bookings/{id}/confirm
POST /api/customer/bookings/{id}/cancel
```

### Tasks

* [x] Rename validation concept to seat hold.
* [x] Store lock owner/user ID.
* [x] Allow the same customer to create booking from held seats.
* [x] Reject locked seats held by another customer.
* [x] Add lock expiry handling.
* [x] Add scheduled job to release expired locks.
* [x] Fix cancel flow to release reserved seats.
* [x] Commit booking flow redesign.

---

## Phase 7: Standard API Response

* [x] Create `ApiResponse<T>`.
* [ ] Use consistent success response:

```json
{
  "success": true,
  "message": "Success message",
  "data": {}
}
```

* [ ] Use consistent error response:

```json
{
  "success": false,
  "message": "Error message",
  "errors": []
}
```

* [x] Update global exception handler.
* [x] Update JWT filter error response.
* [x] Update controllers.
* [x] Commit standard API response.

---

## Phase 8: Testing

* [x] Add auth tests.
* [x] Add movie tests.
* [x] Add hall tests.
* [x] Add show overlap tests.
* [x] Add seat layout generation tests.
* [x] Add booking hold/create/confirm/cancel tests.
* [x] Add authorization tests.
* [x] Add error response tests.
* [x] Add test database config.
* [x] Commit test coverage improvements.

---

## Phase 9: Documentation Update

* [x] Update `README.md`.
* [x] Update `docs/API.md`.
* [x] Update `docs/DATABASE.md`.
* [x] Update `docs/ARCHITECTURE.md`.
* [x] Update `docs/SETUP.md`.
* [x] Add V2 migration notes.
* [x] Commit documentation update.

---

## Phase 10: Docker And CI/CD

* [ ] Add `Dockerfile`.
* [ ] Add `docker-compose.yml`.
* [ ] Add PostgreSQL service in Docker Compose.
* [ ] Add GitHub Actions workflow.
* [ ] Run tests in CI.
* [ ] Commit Docker and CI/CD setup.

---

## Final V2 Goals

* [x] Modular monolith architecture complete.
* [x] PostgreSQL used instead of MySQL.
* [x] Flyway migrations enabled.
* [x] Role-based endpoints cleanly separated.
* [x] Booking lifecycle fixed.
* [x] Environment variables used for secrets.
* [x] API responses standardized.
* [x] Tests added.
* [ ] Docker support added.
* [x] Documentation updated.
