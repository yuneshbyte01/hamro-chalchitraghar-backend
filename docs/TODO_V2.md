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

* [ ] Move `config` package to `shared/config`.
* [ ] Move `security` package to `shared/security`.
* [ ] Move `exception` package to `shared/exception`.
* [ ] Move auth-related controller, service, DTOs to `modules/auth`.
* [ ] Move user-related model, repository, service, DTOs, mapper to `modules/users`.
* [ ] Move movie-related model, repository, service, DTOs, mapper to `modules/movies`.
* [ ] Move hall-related model, repository, service, DTOs, mapper to `modules/halls`.
* [ ] Move show-related model, repository, service, DTOs, mapper to `modules/shows`.
* [ ] Move seat-related model, repository, service, DTOs, mapper to `modules/seats`.
* [ ] Move booking-related model, repository, service, DTOs, mapper to `modules/bookings`.
* [ ] Fix package imports after moving files.
* [ ] Run compile check.

Command:

```powershell
.\mvnw.cmd clean compile
```

* [ ] Commit modular structure.

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
* [ ] Add `/api/customer/bookings/hold`.
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
* [ ] Create PostgreSQL database.
* [ ] Test app connection with PostgreSQL.
* [x] Remove MySQL-specific configuration.
* [ ] Commit PostgreSQL migration.

---

## Phase 4: Flyway Migrations

* [ ] Add Flyway dependency.
* [ ] Change Hibernate from `ddl-auto: update` to `ddl-auto: validate`.
* [ ] Create migration folder:

```text
src/main/resources/db/migration
```

* [ ] Add `V1__create_users_table.sql`.
* [ ] Add `V2__create_movies_table.sql`.
* [ ] Add `V3__create_halls_table.sql`.
* [ ] Add `V4__create_seat_templates_table.sql`.
* [ ] Add `V5__create_shows_table.sql`.
* [ ] Add `V6__create_seats_table.sql`.
* [ ] Add `V7__create_bookings_table.sql`.
* [ ] Add `V8__create_booking_seats_table.sql`.
* [ ] Run migration.
* [ ] Commit Flyway setup.

---

## Phase 5: Environment-Based Configuration

* [ ] Remove hardcoded database username.
* [ ] Remove hardcoded database password.
* [ ] Remove hardcoded JWT secret.
* [ ] Remove hardcoded CORS origin.
* [ ] Add environment variables.
* [ ] Add `.env.example`.
* [ ] Add `application-dev.yaml`.
* [ ] Add `application-prod.yaml`.
* [ ] Commit configuration cleanup.

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

* [ ] Rename validation concept to seat hold.
* [ ] Store lock owner/user ID.
* [ ] Allow the same customer to create booking from held seats.
* [ ] Reject locked seats held by another customer.
* [ ] Add lock expiry handling.
* [ ] Add scheduled job to release expired locks.
* [ ] Fix cancel flow to release reserved seats.
* [ ] Commit booking flow redesign.

---

## Phase 7: Standard API Response

* [ ] Create `ApiResponse<T>`.
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

* [ ] Update global exception handler.
* [ ] Update JWT filter error response.
* [ ] Update controllers.
* [ ] Commit standard API response.

---

## Phase 8: Testing

* [ ] Add auth tests.
* [ ] Add movie tests.
* [ ] Add hall tests.
* [ ] Add show overlap tests.
* [ ] Add seat layout generation tests.
* [ ] Add booking hold/create/confirm/cancel tests.
* [ ] Add authorization tests.
* [ ] Add error response tests.
* [ ] Add test database config.
* [ ] Commit test coverage improvements.

---

## Phase 9: Documentation Update

* [ ] Update `README.md`.
* [ ] Update `docs/API.md`.
* [ ] Update `docs/DATABASE.md`.
* [ ] Update `docs/ARCHITECTURE.md`.
* [ ] Update `docs/SETUP.md`.
* [ ] Add V2 migration notes.
* [ ] Commit documentation update.

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

* [ ] Modular monolith architecture complete.
* [ ] PostgreSQL used instead of MySQL.
* [ ] Flyway migrations enabled.
* [ ] Role-based endpoints cleanly separated.
* [ ] Booking lifecycle fixed.
* [ ] Environment variables used for secrets.
* [ ] API responses standardized.
* [ ] Tests added.
* [ ] Docker support added.
* [ ] Documentation updated.
