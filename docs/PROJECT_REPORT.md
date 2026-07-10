# Project Report

## Project Introduction

Hamro Chalchitraghar Backend is a Java Spring Boot REST API for managing a cinema ticket booking system. The backend supports public discovery of movies, halls, shows, and seat availability, while authenticated users can hold seats, create bookings, confirm bookings, cancel eligible bookings, and view booking history.

Administrative users can manage movies, halls, seat layouts, shows, and users. Staff users can look up bookings. The system is packaged as a Docker-ready modular monolith with PostgreSQL persistence, Flyway migrations, JWT security, Swagger documentation, and integration tests.

## Objectives

| Objective | Implementation |
| --- | --- |
| Provide a clean REST API for cinema booking | Spring MVC controllers grouped by Auth, Public, Customer, Staff, and Admin |
| Protect customer and management actions | Spring Security with JWT bearer tokens and role-based route matchers |
| Prevent conflicting seat bookings | Seat status lifecycle, pessimistic locking, booking checks, and seat hold ownership |
| Keep database schema reproducible | Versioned Flyway SQL migrations |
| Make the project easy to review | Swagger UI, README, architecture docs, database docs, setup docs, and tests |
| Support local and containerized execution | Maven Wrapper, PostgreSQL config, Dockerfile, and Docker Compose |

## Problem Statement

Cinema ticket booking systems need to coordinate public catalog browsing with safe seat reservation behavior. A seat may be viewed by many users at the same time, but it should not be booked by more than one active customer workflow. The backend must therefore provide:

- Public catalog and availability endpoints.
- Secure user authentication.
- Role-aware management endpoints.
- Transactional seat hold and booking operations.
- Database-backed consistency for seats, shows, halls, movies, users, and bookings.

## Technology Selection

| Technology | Reason |
| --- | --- |
| Java 21 | Current LTS Java platform with strong Spring Boot support |
| Spring Boot 3.5.9 | Production-ready REST, configuration, validation, security, and testing support |
| Spring MVC | Direct REST controller model for JSON APIs |
| Spring Security | Standard filter chain and route authorization |
| JWT with JJWT | Stateless authentication suitable for frontend/API separation |
| Spring Data JPA / Hibernate | Repository abstraction and entity mapping |
| PostgreSQL | Reliable relational database for bookings and constraints |
| Flyway | Repeatable, auditable schema migration lifecycle |
| H2 for tests | Fast in-memory integration tests using PostgreSQL compatibility mode |
| Swagger / springdoc-openapi | Interactive API exploration and OpenAPI generation |
| Docker | Portable runtime for backend and database |
| GitHub Actions | Automated test execution on push and pull request |

## Architecture Decisions

The backend is implemented as a modular monolith. This keeps deployment simple while still separating responsibilities into modules.

| Decision | Rationale |
| --- | --- |
| Modular monolith | Avoids distributed-system overhead while preserving clear module boundaries |
| Audience-based controllers | Makes public, customer, staff, and admin surfaces explicit |
| Service-owned business rules | Keeps controllers thin and workflows testable |
| DTOs and mappers | Prevents entities from becoming the external API contract |
| Centralized security config | Keeps authorization rules visible in one place |
| Centralized exception handler | Produces consistent API error responses |
| Flyway migrations | Makes schema history reproducible |
| Pessimistic seat locking | Prevents concurrent writes from claiming the same seat |

## Backend Design

The package structure is split into:

- `applications`: REST controllers grouped by API audience.
- `modules`: domain features such as auth, users, movies, halls, shows, seats, and bookings.
- `shared`: cross-cutting configuration, security, exceptions, response wrapper, and base entity.

Typical request flow:

1. Controller receives the HTTP request.
2. Spring Security authenticates and authorizes the request.
3. Bean Validation validates request DTOs.
4. Service executes business logic.
5. Repository reads or writes entities.
6. Mapper converts entities to response DTOs.
7. Controller returns `ApiResponse<T>`.

## Authentication

Authentication is implemented with email/password login and JWT bearer tokens.

| Action | Implementation |
| --- | --- |
| Register | `POST /api/auth/register` creates a `CUSTOMER` user |
| Password storage | BCrypt hashes |
| Login | `POST /api/auth/login` verifies password and returns JWT |
| Token claims | Subject is email; `role` claim stores role name |
| Refresh | `POST /api/auth/refresh` validates current JWT and returns a new one |
| Request authentication | `JwtAuthenticationFilter` reads `Authorization: Bearer <token>` |

## Authorization

Authorization is configured in `SecurityConfig`.

| API surface | Roles |
| --- | --- |
| Auth | Public |
| Public | Public |
| Customer | CUSTOMER, STAFF, ADMIN |
| Staff | STAFF, ADMIN |
| Admin | ADMIN |
| Swagger/OpenAPI | Public |

The application uses route matchers rather than controller-level `@PreAuthorize` annotations.

## Database Design

The database is relational and centered around users, movies, halls, shows, seats, and bookings.

| Entity | Purpose |
| --- | --- |
| `User` | Authentication identity and role |
| `Movie` | Movie metadata and display status |
| `Hall` | Cinema hall and active/inactive state |
| `SeatTemplate` | Immutable reusable hall layout with validated, dependency-safe regeneration |
| `Show` | Scheduled screening for a movie in a hall |
| `Seat` | Concrete generated seat for a show |
| `Booking` | User booking for a show |
| `BookingSeat` | Join entity connecting bookings and seats |

Every entity extends `GenericEntity`, which provides `id`, `createdAt`, and `updatedAt`.

## Booking Workflow

The booking workflow is intentionally explicit:

1. Admin creates a movie with status `NOW_SHOWING`.
2. Admin creates an active hall.
3. Admin generates a seat layout for the hall.
4. Admin creates a show for the movie and hall.
5. The system generates seats for the show from the hall templates.
6. Customer optionally holds seats for 10 minutes.
7. Customer creates a booking from available seats or seats held by the same user.
8. The booking starts as `INITIATED`, and seats become `RESERVED`.
9. Customer confirms the booking.
10. Booking becomes `CONFIRMED`, and seats become `BOOKED`.

Cancellation is allowed only for `INITIATED` bookings owned by the current user before show time.

## Seat Hold Workflow

Seat holds reduce booking conflicts before a customer creates a booking.

| Rule | Implementation |
| --- | --- |
| Hold duration | 10 minutes |
| Hold state | Seat status `LOCKED` |
| Ownership | `locked_by_user_id` stores the user ID |
| Timestamps | `locked_at` and `lock_expires_at` |
| Concurrency | Pessimistic write lock queries |
| Cleanup | Scheduled job runs every 60 seconds |

If a lock is expired, the service clears it and allows the seat to be held again.

## Flyway

Flyway owns schema creation and evolution. The current migrations create users, movies, halls, seat templates, shows, seats, bookings, booking seats, and the seat lock owner column.

Hibernate validates the schema after Flyway runs, which helps catch mismatches between SQL migrations and JPA mappings.

## Docker

The Docker setup contains:

| Component | Description |
| --- | --- |
| `Dockerfile` | Multi-stage build from Temurin 21 JDK to Temurin 21 JRE |
| `docker-compose.yml` | Starts PostgreSQL 16 and the backend |
| PostgreSQL volume | Persists database data |
| Healthcheck | Waits for PostgreSQL before starting the app |

## CI/CD

The repository includes a GitHub Actions workflow at `.github/workflows/ci.yml`.

The workflow runs on push and pull request:

1. Check out the repository.
2. Set up Java 21 with Temurin.
3. Enable Maven dependency cache.
4. Run `./mvnw -B clean test`.

## Swagger

The backend uses springdoc-openapi.

| Resource | URL |
| --- | --- |
| Swagger UI | `/swagger-ui/index.html` |
| OpenAPI JSON | `/v3/api-docs` |

The OpenAPI configuration defines API metadata, tags, and JWT bearer authentication.

## Testing Strategy

The test suite uses Spring Boot integration tests with MockMvc.

| Test class | Coverage |
| --- | --- |
| `AuthApiIntegrationTest` | Registration, login, duplicate emails, password hashing |
| `AuthorizationAndErrorApiIntegrationTest` | Public access, protected access, role denial, JWT errors, validation errors |
| `CatalogApiIntegrationTest` | Public catalog, admin movie/hall/show flows, seat generation, show validation |
| `BookingApiIntegrationTest` | Seat holds, booking creation, ownership, confirmation, cancellation, duplicate seats |
| `HamroChalchitragharBackendApplicationTests` | Application context startup |

The `test` profile uses H2 in PostgreSQL mode, disables Flyway, and lets Hibernate create/drop the schema for fast isolated tests.

## Challenges Solved

| Challenge | Solution |
| --- | --- |
| Preventing double booking | Seat status checks, active booking queries, and pessimistic write locks |
| Temporary seat ownership | `locked_by_user_id` with lock timestamps |
| Expired holds | Scheduled cleanup and lazy clearing during hold/booking validation |
| Role separation | Centralized Spring Security route matchers |
| Schedule conflicts | Repository query detects overlapping shows in the same hall/date |
| Schema consistency | Flyway migrations plus Hibernate validation |
| API consistency | `ApiResponse<T>` wrapper and centralized exception handling |

## Lessons Learned

The implementation demonstrates that a modular monolith is a strong fit for a portfolio-scale booking backend. It allows the system to model real business flows, enforce transactional consistency, and remain easy to run locally. Keeping controllers thin, placing rules in services, and using explicit SQL migrations made the code easier to document and reason about.

## Final Implementation Summary

Hamro Chalchitraghar Backend is complete as a Spring Boot REST API with:

- Public catalog browsing.
- JWT registration, login, and refresh.
- Role-based CUSTOMER, STAFF, and ADMIN access.
- Admin movie, hall, seat layout, show, and user endpoints.
- Customer seat hold and booking workflows.
- Staff booking lookup.
- PostgreSQL schema managed by Flyway.
- Docker and Docker Compose support.
- Swagger/OpenAPI documentation.
- Integration tests for the core API behavior.
