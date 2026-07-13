# Project Report

## Final notification module

Notifications now include the Notification-1 in-app and ownership foundation, Notification-2 typed
after-commit events, Notification-3 persisted HTML email delivery and retry, and Notification-4
ADMIN operations, customer EMAIL preferences, idempotent show reminders, stale recovery, and bounded
anonymization. Tables are `notifications`, `notification_deliveries`, and
`notification_preferences`. SMS, push, WebSockets, campaigns, tracking, staff history, and customer
deletion remain deferred.


## Show lifecycle and customer safety

Shows use an effective time-based lifecycle in the configured `Asia/Kathmandu` application timezone. A scheduled reconciliation job advances stale shows to `RUNNING` at start and `COMPLETED` at end, while read-time and booking-time checks cover delayed jobs. Public catalogs retain currently running shows but hide ended/cancelled shows; all new holds and booking actions close at show start.

Show schedules become immutable while active seat holds or active (`INITIATED`, `PENDING`, `CONFIRMED`, `BOOKED`) bookings exist. Cancellation blocks active bookings, rejects running/completed shows, and is idempotent once cancelled. Allowed cancellation releases active locks and retains all concrete seats as historical snapshots. Payments, refunds, notifications, and customer rescheduling remain outside this phase.

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
# Booking identity, pricing, and expiry

Bookings now receive immutable `HCG-YYYYMMDD-XXXXXXXX` references with owner-only customer lookup and management lookup routes. Monetary values use exact `BigDecimal` snapshots in NPR: booking seats retain unit prices and bookings retain totals. Initiated reservations have a configurable expiry deadline; lazy lifecycle reconciliation transitions stale records to `EXPIRED`, records lifecycle timestamps, and safely releases eligible reserved seats. Payment, refunds, and confirmed-booking cancellation remain outside the current scope.

Booking reliability now includes batched scheduled expiry, pessimistic booking-row transitions, deterministic seat locking, idempotent confirmation/cancellation, and automatic initiated-booking cancellation when a show is cancelled. A permissive local payment authorization adapter defines the integration boundary for a future gateway without adding payment-provider behavior today. Confirmed bookings still block show cancellation because refunds are not implemented.
## Payment-1 Foundation

The project now includes a standalone payment aggregate linked many-to-one to bookings, exact monetary persistence, server-generated `PAY-YYYYMMDD-XXXXXXXX` references, lifecycle/provider/method enums, a shared repository, centralized mapping, and separate customer/staff/admin response contracts. Read-only APIs allow customers to inspect only their own records while staff and administrators receive safe operational views under existing role policies.

Payment-1 originally created records only through repository/test setup. Booking confirmation continues to use the existing permissive local `PaymentAuthorizationService` compatibility adapter.

Payment-2 adds authenticated owner-only initiation with booking-scoped idempotency, server-authoritative monetary values, one active attempt per booking, configured attempt expiry, centralized lifecycle transitions, cancellation, and a provider adapter boundary. A conditionally enabled LOCAL simulator supports development/test success and failure without external calls. It never confirms bookings. Real providers, verification, callbacks, refunds, cash, and reconciliation remain outside scope.
Payment-3 integrates the eSewa ePay v2 sandbox signed-form flow, signed redirect verification, independent status enquiry, replay-safe finalization, automatic eligible-booking confirmation, late-success manual review, and admin reconciliation. No other gateway, refund, cash, notification, or scheduled reconciliation behavior is included.
Payment-4 completes the operational layer with scheduled eSewa recovery, automatic attempt expiry, paginated admin search, manual-review management, metrics, consistency diagnostics, explicit failure classification, and refund-ready persistence without implementing refunds.
## Ticket domain foundation

The Tickets module persists one server-generated, non-sequential ticket per confirmed booking seat. Issuance is
transactional and idempotent across customer confirmation, verified eSewa completion, and reconciliation through
the same finalizer. Customer APIs are owner-only, while staff and admin receive separate operational read models.
Ticket-1 intentionally does not include QR generation/signing, scanning, check-in, revocation behavior, PDF, or
email delivery; those admission and delivery capabilities remain future work.

Ticket-2 assigns each ticket a random 256-bit opaque QR token. The database stores an AES-256-GCM encrypted form for
authorized on-demand rendering and a unique SHA-256 hash for future scanner lookup, never plaintext. ZXing produces
400x400 PNG images with high error correction without storing image files. QR payloads contain no booking, ticket,
customer, seat, or payment information. Scanning, replay prevention, check-in, rotation, PDF, and delivery remain
deferred.

Ticket-3 adds the staff admission workflow. Raw scanner tokens are hashed for indexed lookup, ticket rows are locked
for atomic check-in, and every attempt is recorded with its actor and operational outcome. Configurable pre-show and
post-show windows constrain admission. Terminal ticket states, cancelled/unconfirmed bookings, cancelled shows,
early/late arrival, unknown tokens, and replay are rejected; concurrent scans produce exactly one admission.
Customer APIs reveal only check-in state, while staff/admin detail includes validation history.

Ticket-4 completes the online ticket lifecycle with admin revocation and QR rotation, scheduled expiry, protected
single/bundle PDF downloads, booking-level after-commit email delivery and retry persistence, paginated operational
search, state/validation metrics, and consistency detection. PDFs are generated with OpenPDF and embedded ZXing QR
images entirely in memory. Cancellation revokes issued tickets while preserving checked-in tickets for manual
review. Offline validation, external storage, SMS/push, and generic audit logging remain outside scope.
# Notification-1

Notification-1 adds the in-app notification domain and Flyway persistence, stable type/channel
enums, validated optional JSON metadata, per-user/channel event-key idempotency, centralized safe
DTO mapping, and customer-owned list/detail/read/read-all/unread-count APIs. Lists are bounded,
filterable, and stably ordered; all access and mutations are owner-bound, including when staff or
admin accounts use customer routes. Notification timestamps use the configured application `Clock`.

This phase does not automatically create notifications from registration, authentication, booking,
payment, show, or ticket workflows. It does not alter password-reset or ticket email delivery.
Business events are deferred to Notification-2; email/async/retry work to Notification-3; reminders,
preferences, and administrative operations remain later work. There is no historical backfill.
# Notification-2

Notification-2 adds typed events for registration, booking lifecycle, payment success/failure,
show lifecycle, and ticket issuance. Cohesive after-commit listeners create idempotent IN_APP
notifications through the existing Notification-1 service and uniqueness constraint. Integration
occurs at authoritative state transitions, providing rollback safety and duplicate-processing
protection while preserving customer ownership and existing read APIs. Email notifications,
reminders, async delivery, and an outbox are explicitly deferred.

## Notification-3

Notification-3 adds a database-backed general email queue, recipient snapshots, HTML Thymeleaf
templates, a bounded executor, concurrency-safe claims, exponential retry, stale-claim recovery,
sanitized failures, and disabled-environment audit rows. External mail failure is isolated from
business and in-app notification commits. Existing ticket PDF delivery and password-reset OTP email
remain separate; admin delivery operations, preferences, analytics, retention, and reminders remain
deferred.
# Audit-1

Audit-1 establishes append-only persistence, safe allowlisted snapshots, bounded filtering and
pagination, ADMIN-only summary/detail visibility, Swagger descriptions, and authorization and
integration coverage. Automatic auditing of authentication and business modules remains deferred.
# Audit-2 delivery

Audit-2 activates typed after-commit auditing across authentication and selected authoritative user,
catalog, show, booking, payment, ticket, and notification transitions. Event-ID idempotency, safe
snapshots, isolated selected failures, and append-failure isolation are included. Request tracing is
deferred to Audit-3.
# Audit-3 delivery

Audit-3 adds request/correlation headers, MDC context, safe method/path and optional masked client
metadata, centralized invalid-JWT/access-denied auditing, actor MDC enrichment, ticket request-ID
compatibility, and email-executor propagation. Export, retention, anonymization, integrity controls,
dashboards, and distributed tracing remain deferred.
