# Maintenance Guide

## Maintaining business reports

Add KPIs inside `modules.reporting`, expose database aggregate projections from
`ReportingQueryRepository`, and map them in read-only services. Controllers should only parse
parameters, build the Clock-derived range, and wrap `ApiResponse`. Never use `findAll()` or lazy
entity traversal for KPIs. Monetary aggregates must use `BigDecimal`, group by currency, preserve
zero/null handling, and avoid joins that multiply amounts. Date predicates remain
`>= startInclusive` and `< endExclusive`. Every KPI needs empty, exact-boundary, authorization, and
OpenAPI tests using Clock-derived or explicit deterministic timestamps, including Kathmandu/UTC
boundary coverage.

## Notification-4 operations

Preference types belong in the centralized preference policy and affect only future EMAIL rows.
Reminder eligibility requires an enabled user, CONFIRMED booking, SCHEDULED future show, configured
window, and deterministic `SHOW_REMINDER:{bookingId}:{duration}` key. Manual retry accepts FAILED
rows below their persisted maximum only. Investigate EXHAUSTED rows through masked admin APIs. Stale
claims preserve attempt counts. Retention is bounded anonymization, never deletion. Use injected
`Clock`, row locks, safe logs, and never expose message bodies, recipients, tokens, or credentials.


This guide describes how to extend and maintain Hamro Chalchitraghar Backend while staying consistent with the current implementation.

## Development Workflow

## Observability maintenance

- Keep Actuator exposure limited to `health`, `info`, and `prometheus`; never use wildcard exposure.
- Keep liveness independent of databases, SMTP, eSewa, schedulers, and workers.
- Add a readiness dependency only when its loss prevents core requests from being served.
- Health indicators must be bounded, read-only, non-destructive, and must not expose configuration details.
- Keep Prometheus ADMIN-protected until a private monitoring network provides equivalent access control.
- Prefer built-in JVM, process, HTTP, datasource and Hikari meters over custom duplicates.
- Keep shutdown waits bounded. Persisted notification/refund state is the recovery mechanism for interruption.
- Never log passwords, OTPs, reset tokens, JWTs, QR tokens, signatures, credentials, or provider payloads.
- Custom business/job metrics, structured logging, dashboards, alerts, and tracing belong to later phases.


1. Create or update code in the relevant module under `src/main/java/com/chalchitraghar/modules`.
2. Add or update the controller under the correct audience package in `applications`.
3. Add DTO validation with Jakarta Bean Validation.
4. Add mapper logic if entities cross the API boundary.
5. Add repository queries only where derived queries are not enough.
6. Add integration tests with MockMvc.
7. Run `./mvnw clean test`.
8. Update documentation when public behavior, schema, or setup changes.

## Add a New Module

Create a package under:

```text
src/main/java/com/chalchitraghar/modules/<module>
```

Use the existing module layout where applicable:

```text
<module>
├── dto
│   ├── request
│   └── response
├── entity
├── enums
├── mapper
├── repository
├── service
│   └── impl
```

Add controllers under `applications` based on audience:

| Audience | Package |
| --- | --- |
| Public | `applications.publicapi` |
| Customer | `applications.customer` |
| Staff | `applications.staff` |
| Admin | `applications.admin` |
| Auth | `applications.auth` |

## Add an Endpoint

1. Add a method to the correct controller.
2. Use `@GetMapping`, `@PostMapping`, `@PutMapping`, or `@DeleteMapping`.
3. Use `@Valid @RequestBody` for request DTOs.
4. Return `ResponseEntity<ApiResponse<T>>` for JSON responses.
5. Add `@Operation` and `@Tag` metadata for Swagger.
6. Rely on `SecurityConfig` request matchers for role access.
7. Add an integration test for success and expected failure cases.

Example controller pattern:

```java
@PostMapping
public ResponseEntity<ApiResponse<AdminMovieDetailResponse>> createMovie(@Valid @RequestBody MovieRequest dto) {
    AdminMovieDetailResponse created = movieService.addMovie(dto);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Movie created successfully", created));
}
```

## Add a Migration

Create a new SQL file in:

```text
src/main/resources/db/migration
```

Naming format:

```text
V<number>__short_description.sql
```

Rules:

| Rule | Reason |
| --- | --- |
| Never edit applied migrations in shared databases | Flyway tracks checksums |
| Add indexes and constraints explicitly | Keeps database behavior visible |
| Keep entity annotations aligned with SQL | Hibernate validates schema |
| Run tests after adding migrations | Detects mapping and startup issues |

## Add a DTO

Keep response DTOs separated by audience when visibility or field exposure differs. Shows use public summary/detail contracts without audit fields and admin summary/detail contracts with full administrative nested DTOs. Controllers must delegate conversion to `ShowMapper`; public and admin show reads must call their separate `ShowService` methods so public visibility rules cannot affect historical admin access.

Bookings follow the same audience boundary. Customer list/detail operations use `CustomerBookingSummaryResponse` and `CustomerBookingDetailResponse` and must always enforce ownership. Staff and admin use the dedicated `StaffBookingDetailResponse` and `AdminBookingDetailResponse` contracts through `/api/staff/bookings/{bookingId}` and `/api/admin/bookings/{bookingId}`. Keep all conversions in `BookingMapper`; never restore a role bypass inside customer service reads or expose customer identity and audit timestamps through customer DTOs.

Booking history is paginated with the shared `PageResponse<T>`. Keep filters in `BookingSearchCriteria` and `BookingSpecification`, apply customer ownership in the database query, and retain the booking sort allowlist (`id`, `bookingTime`, `status`, `createdAt`, `updatedAt`). Staff/admin search must remain limited to customer name/email, movie title, and hall name. Batch-load booking seats for list pages and let `BookingMapper` order seat codes by `positionIndex`.

Booking prices are currency-safe snapshots: copy `Seat.price` into `BookingSeat.unitPrice`, sum with `BigDecimal` into `Booking.totalAmount`, and never map historical prices from the mutable seat. Generate references only through `BookingReferenceGenerator`. Route all initiated expiry checks through `BookingLifecycleService` using the application `Clock`; confirmation, cancellation, and expiry timestamps must not use an independent system clock.

Keep scheduled expiry bounded by `BOOKING_EXPIRY_BATCH_SIZE` and scheduled by `BOOKING_EXPIRY_CLEANUP_INTERVAL_MS`. Never duplicate expiry seat-release logic in a job or controller. State transitions must lock the booking row before status/expiry checks and sort seat IDs before pessimistic locking. Confirmation must pass through `PaymentAuthorizationService`. Repeated confirmed confirmation and cancelled cancellation are idempotent; confirmed paid cancellation must use the Refund-2 orchestration.

Paginated show lists use `ShowSearchCriteria`, `ShowSpecification`, and the shared `PageResponse<T>`. Add new show filters in the criteria/specification rather than controllers, keep the sort-field allowlist in `ShowServiceImpl`, and apply public visibility predicates in the database query before pagination.

Keep scheduling and lifecycle decisions in `ShowServiceImpl`. `SHOW_BUFFER_MINUTES` configures the cleaning gap (default 15), while `SHOW_DURATION_TOLERANCE_MINUTES` configures the movie-duration tolerance (default 5). Any lifecycle extension must update the centralized transition guard, the status endpoint documentation, and scheduling integration tests; do not bypass it by assigning `Show.status` in controllers.

Use the injected application `Clock` and `ShowLifecycleService` for time-based show decisions; do not add independent `now()` checks to booking or seat workflows. `SHOW_STATUS_RECONCILIATION_INTERVAL_MS` controls the forward-only reconciliation job and `APP_TIME_ZONE` defines the single cinema timezone. Keep the active booking status list centralized in show management when changing dependency policy.

Place request DTOs under:

```text
modules/<module>/dto/request
```

Place response DTOs under:

```text
modules/<module>/dto/response
```

Use validation annotations on request DTOs:

| Annotation | Use |
| --- | --- |
| `@NotBlank` | Required strings |
| `@NotNull` | Required object values |
| `@Email` | Email addresses |
| `@Size` | Length limits |
| `@Future` | Future dates |
| `@Positive` / `@PositiveOrZero` | Numeric constraints |

## Add an Entity

1. Extend `GenericEntity` for `id`, `createdAt`, and `updatedAt`.
2. Annotate with `@Entity` and `@Table`.
3. Match table and column names to Flyway SQL.
4. Use `@Enumerated(EnumType.STRING)` for enums.
5. Use explicit `@JoinColumn` for relationships.
6. Add validation annotations matching the domain rules.

Entity defaults can be set with `@PrePersist`, as seen in `Show`, `Seat`, and `Booking`. Hall status is supplied by the admin request and is not forced during persistence.

## Add a Repository

Create an interface under:

```text
modules/<module>/repository
```

Extend `JpaRepository<Entity, Long>`.

Use derived queries for simple reads. Use `@Query` for domain-specific checks, such as overlapping shows or active bookings.

For concurrent seat updates, use pessimistic locking as in `SeatRepository`:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.id IN :seatIds")
List<Seat> findByShowIdAndSeatIdsWithLock(Long showId, List<Long> seatIds);
```

## Add a Service

1. Define the service interface under `service`.
2. Implement it under `service.impl`.
3. Annotate implementation with `@Service`.
4. Use `@Transactional` for write workflows.
5. Throw domain exceptions from `shared.exception` where possible.
6. Keep HTTP concerns out of services.

Services should own business rules. Controllers should remain thin.

### Seat-template browsing

Admin template inspection uses `GET /api/admin/halls/{hallId}/seat-layout`. Keep its search, `seatType` and row filters, allowlisted sorting, and validation in `SeatLayoutService`; the controller should only collect query parameters. `AdminSeatLayoutResponse` statistics (`totalSeats`, category counts, and `rows`) describe the filtered template result.

Do not apply this admin browsing model to `GET /api/public/shows/{showId}/seats`. That endpoint returns concrete `SeatResponse` records and is intentionally non-paginated and ordered by `positionIndex`, allowing the frontend to render the complete auditorium map.

Fixed seat generation must retain the build-validate-persist sequence: create the full collection, assign codes through `SeatTemplate.refreshSeatCode`, validate it with `SeatTemplateValidator`, then call `saveAll` within the transaction. Never persist templates incrementally. Validation requires uppercase one- or two-letter rows, seat numbers from `1` to `100` that are sequential per row, codes equal to row plus number, and zero-based gapless positions. The generated count must match hall capacity.

Keep Flyway and entity uniqueness metadata aligned for template keys `(hall_id, seat_code)`, `(hall_id, row_label, seat_number)`, `(hall_id, position_index)` and their show-seat equivalents. Generated show-seat prices must come from `SeatPricingPolicy`; do not duplicate category prices in services.

Seat templates are immutable API resources: do not add partial update or delete operations. Safe regeneration must remain all-or-nothing and must reject every hall with any show, including historical or cancelled shows. Concrete seats are show snapshots and must never be silently regenerated when a show changes hall; hall changes are rejected once seats or bookings exist.

Show cancellation preserves concrete seats. Public reads, holds, and booking creation use show status as the availability boundary, while expired public locks may be safely normalized transactionally. `SeatStatus.CANCELLED` is intentionally unused.

## Add a Controller

Controller conventions:

| Convention | Current pattern |
| --- | --- |
| Package | `applications.<audience>` |
| Base path | `/api/<audience-or-public>/<resource>` |
| Response wrapper | `ApiResponse<T>` |
| Validation | `@Valid` |
| Swagger | `@Tag`, `@Operation`, `@SecurityRequirement` for protected controllers |
| Current user | Read from `SecurityContextHolder` where needed |

## Add Tests

Integration tests live in:

```text
src/test/java/com/chalchitraghar
```

Use `AbstractIntegrationTest` for:

- MockMvc
- ObjectMapper
- Repositories
- Test data helpers
- Login token helpers
- Database cleanup

Test coverage should include:

| Area | Examples |
| --- | --- |
| Success path | Endpoint returns expected `ApiResponse` |
| Validation | Missing or invalid DTO fields |
| Authorization | Role cannot access restricted routes |
| Domain rules | Seat conflicts, duplicate seats, show overlap |
| Persistence side effects | Seat status changes, booking status changes |

Run tests:

```bash
./mvnw clean test
```

## Run Flyway

Flyway runs automatically during application startup for non-test profiles.

To verify migrations:

```bash
./mvnw spring-boot:run
```

The application should start only if Flyway succeeds and Hibernate validates the schema.

## Run Docker

```bash
docker compose up --build
```

Inspect logs:

```bash
docker compose logs app
docker compose logs postgres
```

Stop:

```bash
docker compose down
```

## Run CI Locally

The GitHub Actions CI workflow runs:

```bash
./mvnw -B clean test
```

Run the same command locally before pushing.

## Coding Conventions

| Concern | Convention |
| --- | --- |
| Java version | Java 21 |
| Constructor injection | Lombok `@RequiredArgsConstructor` |
| DTOs | Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor` where useful |
| Responses | Standard `ApiResponse<T>` |
| Pagination | Use `PageResponse<T>` inside `ApiResponse<T>` for paginated list endpoints |
| Exceptions | Centralized by `GlobalExceptionHandler` |
| Authorization | Centralized in `SecurityConfig` |
| Passwords | BCrypt only |
| JWT claims | Subject is email, `role` claim is role name |
| JWT validity | Reject tokens issued before `password_changed_at` |
| Entity enums | Stored as strings |
| Soft delete | Status updates, not row deletion, for movies, halls, and shows |
| Schema changes | Flyway migration first, entity mapping second |

## Authentication Hardening

Account status is tracked on `users`:

- `enabled=false` blocks local login, Google login, and protected endpoint access with existing JWTs.
- Wrong local passwords increment `failed_login_attempts`.
- 5 failed local password attempts set `locked=true` and `locked_until=now+15 minutes`.
- Manual admin locks set `locked=true` and `locked_until=null`, so they do not expire automatically.
- Admin unlock sets `locked=false`, clears `locked_until`, and resets `failed_login_attempts=0`.
- Active lockouts return `Account is temporarily locked. Please try again later.`
- Successful local or Google login resets failed attempts, clears expired lock state, and updates `last_login_at`.
- Google token verification failures do not increment local password failure counters.
- Registration, profile password change, and OTP reset update `password_changed_at`.
- JWTs issued before `password_changed_at` return `Token is no longer valid after password change`.

## Admin User List Maintenance

`GET /api/admin/users` is intentionally paginated and filterable so it remains safe on large user tables.

Supported query parameters:

| Parameter | Purpose |
| --- | --- |
| `page`, `size` | Bound result windows; defaults are `0` and `20` |
| `sortBy`, `sortDir` | Sort by an allowlisted field in `asc` or `desc` direction |
| `search` | Case-insensitive match against user `name` or `email` |
| `role` | Filter by `CUSTOMER`, `STAFF`, or `ADMIN` |
| `authProvider` | Filter by `LOCAL` or `GOOGLE` |
| `enabled`, `locked`, `emailVerified` | Filter by account booleans |

Allowed sort fields are `id`, `name`, `email`, `role`, `enabled`, `locked`, `authProvider`, `createdAt`, `updatedAt`, and `lastLoginAt`.

Keep user list changes inside `UserService`, `UserSpecification`, `UserMapper`, and the admin controller. Do not expose `password`, `googleId`, OTP hashes, or other internal authentication data in list or detail DTOs.

## Hall List Maintenance

`GET /api/public/halls` and `GET /api/admin/halls` are paginated and searchable. Both return `PageResponse<T>` inside the standard `ApiResponse<T>` wrapper.

Public hall lists use `PublicHallSummaryResponse` and must not expose `layoutRef`, `createdAt`, or `updatedAt`. Admin hall lists use `AdminHallSummaryResponse`; admin detail, create, and update responses use `AdminHallDetailResponse`.

Public hall visibility is intentionally narrower than admin visibility:

- Public lists return only `ACTIVE` halls.
- Public detail treats `INACTIVE` halls as not found.
- Admin list/detail endpoints can see both `ACTIVE` and `INACTIVE` halls.

Supported query parameters:

| Parameter | Public | Admin |
| --- | --- | --- |
| `page`, `size` | Bound result windows; defaults are `0` and `20` | Bound result windows; defaults are `0` and `20` |
| `sortBy`, `sortDir` | Default `name,asc` | Default `createdAt,desc` |
| `search` | Case-insensitive match against hall `name` | Case-insensitive match against hall `name` or `layoutRef` |
| `status` | Not supported because public halls are `ACTIVE` only | Filter by `ACTIVE` or `INACTIVE` |

Allowed sort fields are `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, and `updatedAt`.

Hall create and update validation rules:

- Hall names are trimmed and must be unique case-insensitively.
- Duplicate hall names return `409`.
- Capacity must be between `1` and `1000`.
- `layoutRef` is trimmed, required, at most `100` characters, and may contain only letters, numbers, hyphen, and underscore.
- New halls may be created as `ACTIVE` or `INACTIVE`.
- Allowed status changes are `ACTIVE -> INACTIVE` and `INACTIVE -> ACTIVE`.
- The database enforces normalized hall name uniqueness with `uk_halls_name_normalized` on `lower(trim(name))`.
- Hall delete is a soft inactivation; rows are not physically removed.
- Inactivation by delete or update is rejected with `409` when future active `SCHEDULED` or `RUNNING` shows exist for the hall.
- Once seat templates exist, `capacity` and `layoutRef` cannot change. Keep name-only edits and reactivation available.
- Seat layout generation is allowed only for `ACTIVE` halls without existing templates.
- The current generator is fixed at 188 seats, so generation requires `capacity=188` until dynamic layout generation is implemented.

Keep hall list, lifecycle, show dependency, and seat template dependency changes inside `HallService`, `SeatLayoutService`, `HallSpecification`, `HallMapper`, and the required repository dependency methods. Do not physically delete halls, auto-cancel shows, add `deletedAt`, add audit logging, or redesign the seat layout generator as part of hall maintenance.

## Movie List Maintenance

`GET /api/public/movies` and `GET /api/admin/movies` are paginated and filterable. Both return `PageResponse<T>` inside the standard `ApiResponse<T>` wrapper.

Public movie lists use `PublicMovieSummaryResponse` and must not expose `description`, `createdAt`, or `updatedAt`. Admin movie lists use `AdminMovieSummaryResponse`; admin detail, create, and update responses use `AdminMovieDetailResponse`.

Public movie visibility is intentionally narrower than admin visibility:

- Public lists return only `UPCOMING` and `NOW_SHOWING` movies by default.
- Public `status=ENDED` filters are rejected with `400`.
- Public detail treats `ENDED` movies as not found.
- Admin list/detail endpoints can see all statuses, including `ENDED`.

Supported query parameters:

| Parameter | Purpose |
| --- | --- |
| `page`, `size` | Bound result windows; defaults are `0` and `20` |
| `sortBy`, `sortDir` | Sort by an allowlisted field in `asc` or `desc` direction |
| `search` | Case-insensitive match against movie `title`, `genre`, or `language` |
| `status` | Filter by `UPCOMING`, `NOW_SHOWING`, or `ENDED` |
| `genre`, `language` | Case-insensitive exact filters |
| `releaseDateFrom`, `releaseDateTo` | Inclusive release date range filters in `yyyy-MM-dd` format |

Allowed sort fields are `id`, `title`, `genre`, `language`, `releaseDate`, `status`, `createdAt`, `updatedAt`, and `durationMinutes`.

Movie create and update validation rules:

- Duplicate movies are rejected when normalized title (`lower(trim(title))`) and `releaseDate` match an existing movie.
- The database enforces the same duplicate rule with `uk_movies_title_normalized_release_date`.
- `durationMinutes` must be between 1 and 600.
- `posterUrl` must be an absolute `http` or `https` URL and at most 500 characters.
- `UPCOMING` requires `releaseDate` today or later.
- `NOW_SHOWING` and `ENDED` require `releaseDate` today or earlier.
- Allowed status transitions are `UPCOMING -> NOW_SHOWING`, `NOW_SHOWING -> ENDED`, and `UPCOMING -> ENDED`.
- Ending or deleting a movie is rejected with `409` when future active shows exist.
- Future active shows are `SCHEDULED` or `RUNNING` shows whose show window has not fully passed.
- Movie delete is a soft delete: it sets `status=ENDED`; it does not remove the row.

Keep movie list and lifecycle changes inside `MovieService`, `MovieSpecification`, `MovieMapper`, `ShowRepository`, and the public/admin movie controllers. Do not physically delete movies, auto-cancel shows, add `deletedAt`, change image storage, or add audit logging as part of movie visibility or soft-delete maintenance.

## Admin Account State Maintenance

Manual account state endpoints are ADMIN-only:

| Endpoint | State change |
| --- | --- |
| `POST /api/admin/users` | Creates a `LOCAL` `CUSTOMER`, `STAFF`, or `ADMIN` user with a BCrypt password |
| `PUT /api/admin/users/{id}` | Updates `name`, `role`, `enabled`, and `emailVerified` only |
| `PUT /api/admin/users/{id}/enable` | Sets `enabled=true` |
| `PUT /api/admin/users/{id}/disable` | Sets `enabled=false` |
| `PUT /api/admin/users/{id}/lock` | Sets `locked=true` and `locked_until=null` |
| `PUT /api/admin/users/{id}/unlock` | Sets `locked=false`, clears `locked_until`, resets `failed_login_attempts=0` |

Operational notes:

- Admins cannot disable or lock their own account.
- Admins cannot remove their own `ADMIN` role.
- The last enabled admin cannot be disabled, locked, or changed to another role.
- Disabled users cannot log in and cannot keep using existing JWTs because `JwtAuthenticationFilter` reloads the user and checks `enabled` on every protected request.
- Manual locks and Auth-5 automatic lockouts share `locked`; `locked_until=null` means manual lock, while a future `locked_until` means automatic temporary lockout.
- Unlock clears both manual and automatic lock state.
- Lifecycle updates must not expose or accept password, auth provider, Google metadata, failed login counters, lock expiry, last login, or password change timestamps.
- Keep last-admin protection in `UserServiceImpl`; the repository count helper is `countByRoleAndEnabledTrue(Role.ADMIN)`.

LOCAL user lifecycle:

- Customer self-registration creates `LOCAL` customers through the auth module.
- Admin creation can create `CUSTOMER`, `STAFF`, or `ADMIN` local users.
- Admin-created users start with `email_verified=false`, `enabled=true`, `locked=false`, `failed_login_attempts=0`, `last_login_at=null`, and `password_changed_at=now`.
- Passwords are stored only as BCrypt hashes.

## Git Workflow

1. Branch from the current main development branch.
2. Keep commits focused.
3. Run tests before opening a pull request.
4. Include verification notes in the pull request.
5. Avoid mixing documentation-only changes with Java behavior changes unless the behavior requires doc updates.

## Current Domain Rules to Preserve

| Domain | Rule |
| --- | --- |
| Registration | New users are created with `CUSTOMER` role |
| Login | Passwords are checked with BCrypt |
| Login | 5 failed local password attempts lock the account for 15 minutes |
| Login | Disabled or actively locked accounts cannot authenticate |
| Admin users | Admins cannot disable or lock their own account |
| Admin users | Admins cannot remove their own `ADMIN` role |
| Admin users | At least one enabled `ADMIN` must remain |
| Admin users | Unlock clears lock expiry and failed login attempts |
| JWT | Tokens issued before the latest password change are rejected |
| Shows | Can be scheduled only for `NOW_SHOWING` movies |
| Shows | Cannot be scheduled in `INACTIVE` halls |
| Shows | Cannot overlap another non-cancelled show in the same hall/date |
| Seat templates | Generated once per hall |
| Show seats | Generated once per show from hall templates |
| Seat hold | Holds last 10 minutes |
| Seat hold | Held seats are owned by `locked_by_user_id` |
| Booking creation | Allows available seats or seats held by the same user |
| Booking confirmation | Only `INITIATED` bookings can be confirmed |
| Booking cancellation | Only owner can cancel an `INITIATED` booking before show time |
## Payment-1 Maintenance Rules

- Keep payment business logic in `modules/payments`; controllers only authenticate, delegate, and wrap responses.
- Generate references only through `PaymentReferenceGenerator`; never accept a payment reference from a request body.
- Preserve exact `BigDecimal`/`NUMERIC(12,2)` amounts and three-character currencies.
- Keep customer, staff, and admin DTOs separate and mapping centralized in `PaymentMapper`.
- Customer lookups must remain owner-only and return `404` for missing and non-owned references.
- Payment-1 endpoints are read-only. Do not create payment rows during booking confirmation.
- Do not treat the placeholder status/provider/method enums as implemented provider behavior.
- Never persist credentials, payment secrets, sensitive account/card data, or raw provider payloads in `payments`.

Payment-2 initiation must keep `(booking_id, idempotency_key)` behavior stable, derive amount/currency only from the locked booking, and enforce at most one active `CREATED`/`PENDING` attempt. All state changes must pass through `PaymentLifecycleService`; process and cancel operations lock the payment row. Keep real provider network work outside locking transactions. `PAYMENT_LOCAL_ENABLED` must remain false in production.
For eSewa, never log `ESEWA_SECRET_KEY`, never accept merchant/form fields from customers, and never hold database locks during status HTTP calls. Keep request signed fields ordered as `total_amount,transaction_uuid,product_code`. Any verified late success must remain `SUCCESS` with manual review instead of modifying an ineligible booking.
Keep reconciliation bounded by `PAYMENT_RECONCILIATION_BATCH_SIZE` and scheduled with `PAYMENT_RECONCILIATION_INTERVAL_MS`. A failure for one payment must not stop the batch. Never reconcile terminal payments or mark transient network failures as failed. Review consistency results operationally; diagnostics do not silently mutate bookings or payments.
## Ticket maintenance

Keep ticket issuance attached to the authoritative transition to `BookingStatus.CONFIRMED`, after seats become
`BOOKED`; never attach it to `PaymentStatus.SUCCESS` alone. All confirmation paths must use
`TicketIssuanceService`, and the unique `tickets.booking_seat_id` constraint must remain the final duplicate guard.
Use the injected application `Clock` for `issuedAt`. Existing confirmed development bookings can be reconciled with
the internal `backfillConfirmedBooking` service method; do not create tickets silently during reads.

Ticket response changes must preserve customer/staff/admin DTO separation. Do not expose database IDs, customer
identity in customer responses, QR internals, authentication data, or payment credentials. QR generation, scanning,
check-in, revocation transitions, PDF, and email delivery are outside Ticket-1.

QR tokens must remain opaque, random, and free of domain data. Persist only AES-256-GCM ciphertext and a unique
SHA-256 lookup hash. Never log plaintext tokens, ciphertext, hashes, or encryption keys. Preserve random IVs,
authenticated key/version metadata, owner-only QR rendering, and `private, no-store` responses. Key rotation and
token regeneration require an explicit future workflow; do not overwrite existing ticket material during reads or
idempotent issuance.

Admission must remain an online, single-transaction operation using the locked QR-hash lookup. Never split state
validation, `CHECKED_IN` mutation, and validation-history creation across transactions. Preserve the only supported
transitions from `ISSUED` to `CHECKED_IN`, `REVOKED`, or `EXPIRED`; all destination states are terminal. Use the
injected `Clock` and configured entry/grace windows. Every expected rejection—including unknown tokens and replay—
must create a validation record without changing the ticket.

Revocation and reissue must retain row locks and terminal-state rules. Never create a second ticket during reissue;
rotate ciphertext/hash in place and preserve the reference. Keep all show cancellation paths routed through ticket
orchestration. Expiry processes only `ISSUED` rows in bounded batches while scan-time checks remain authoritative.
PDFs and QR images remain memory-only. Delivery is triggered after commit, uniquely keyed by booking/channel, and
must never roll back payment, confirmation, or issuance. Search sorts must stay allowlisted, and consistency findings
must not be silently repaired.
# Notification maintenance

- Add new values to `NotificationType` only with a stable name, migration/API compatibility review,
  and tests.
- Event keys must be deterministic, non-blank, free of secrets, and unique for the intended user and
  channel, for example `BOOKING_CONFIRMED:{bookingId}`.
- Payload is optional validated JSON metadata, not an authorization source. Never store passwords,
  OTPs, reset/JWT/Google tokens, payment secrets or full gateway responses, SMTP credentials, raw QR
  material, cryptographic keys, stack traces, or full email bodies.
- Customer repository operations must bind notification ID and authenticated user ID in the query.
  Do not fetch globally and enforce ownership only in controllers.
- All notification timestamps must use the injected application `Clock`. Do not introduce direct
  system-time calls in notification code or fixtures.
- Never create or reconcile notifications during reads. Notification-2 will create rows only for new
  events unless a separate backfill is explicitly approved.
- Schema changes require Flyway updates plus entity/migration constraint and integration tests.
- Preserve the separation between in-app read state and future channel delivery state. Do not merge
  this model with `TicketDelivery`.
# Adding a notification business event

Add an immutable event under `modules.notifications.event`, containing only the recipient ID,
public-safe context, and an injected-`Clock` occurrence time. Define its deterministic key and text
in `NotificationContentFactory`, add an `AFTER_COMMIT` listener method, and publish only after the
authoritative state transition succeeds but before its transaction returns. Never pass JPA entities,
tokens, OTPs, gateway payloads, QR data, or credentials. Tests must prove commit creation, rollback
suppression, duplicate delivery idempotency, ownership, and UTC/Kathmandu independence. Do not put
timestamps or random values in idempotency keys.

## Maintaining notification email

Enable a type only in `NotificationEmailPolicy`, add its escaped Thymeleaf template under
`templates/email`, and test with a mocked `NotificationMailSender`. Never render raw HTML or place
tokens, OTPs, QR values, gateway payloads, credentials, or exception text in templates or delivery
rows. SMTP work must remain outside business and claim transactions. Treat mail/connectivity
exceptions as transient; invalid recipients, missing templates, and construction errors are
permanent. Inspect `EXHAUSTED` rows operationally, and recover stale `PROCESSING` rows through the
retry processor. All timestamps and fixtures must use the injected `Clock`.
# Maintaining audit logs

Audit records are append-only: never add update/delete service or controller operations. Add future
`AuditAction` constants only with a stable, migration-compatible name. Snapshots must be constructed
explicitly as maps, use keys in `AuditSnapshotValidator`'s allowlist, contain no entity/request-body
serialization, and remain under 16 KB per field. Secret, credential, password, OTP, token, payment
signature/payload, email-body, binary, stack-trace, and SQL keys are rejected recursively and without
case sensitivity. All timestamps use the injected `Clock`. Add filters in the filter record and
specification together, preserve the stable two-column sort, and verify each next-numbered migration
against both the entity mapping and PostgreSQL syntax.
# Audit-2 maintenance

Publish immutable scalar events inside the authoritative transaction and persist success only after
commit. Use stable IDs for transition delivery and unique IDs for genuine attempts. Build explicit,
deterministic allowlisted maps; deliberately assign USER/SYSTEM/EXTERNAL/ANONYMOUS actors; test
rollback; use the injected `Clock`; and never audit the same action in both controller and service.
# Audit-3 maintenance

# Audit-4 maintenance

Export columns must remain explicitly allowlisted, omit raw JSON/IP/user-agent/body data, and pass every
text cell through the control-character and formula-injection sanitizer. Reports must keep bounded ranges
and low-cardinality groupings. Change retention only through `AuditRetentionService`; never make core fields
mutable or add controller update/delete operations. Anonymizable fields and canonical integrity fields must
remain disjoint. Hard deletion would erase event-ID deduplication history and is intentionally unsupported.
All new cutoffs, buckets, and timestamps must use the injected `Clock`. A hash mismatch signals investigation,
not proof of malicious tampering.

Request context is immutable and must be cleared with MDC in `finally`. Add only bounded low-risk
fields, never servlet objects or bodies. Keep security reason codes stable and use the per-request
deduplication marker. Async executors must decorate tasks and restore worker state; scheduled jobs use
fresh SYSTEM contexts. All context timestamps use the injected `Clock`. Forwarded headers remain
untrusted unless deployment topology guarantees a trusted proxy boundary.

## Refund-1 maintenance rules

- Treat `REQUESTED` as an intent that reserves balance, never as proof of returned money.
- Support only full refunds and derive amount/currency from the locked successful payment.
- Lock Payment before Booking, recheck idempotency after locking, and aggregate all balance-reserving statuses.
- Use stable, secret-free idempotency keys such as `SHOW_CANCELLATION:{bookingId}:{showId}`.
- Reject checked-in tickets and reason/state mismatches; never repair inconsistent aggregates implicitly.
- Generate references only through `RefundReferenceGenerator` and use the injected `Clock` for every refund timestamp.
- Do not mutate refund entities directly outside authoritative services or mark `PaymentStatus.REFUNDED` in Refund-1.
- Never call a provider while holding database locks or persist/log raw provider payloads, signatures, tokens, or credentials.
- Customer lookups must remain owner-scoped because STAFF and ADMIN may also enter `/api/customer/**`.

### Refund-2 workflow invariants

## Observability-2 maintenance

- Name custom meters `chalchitraghar.<domain>.<operation>` and treat released names as contracts.
- Use only enum/fixed tags such as operation, outcome, job, executor, provider, method, or channel.
- Never tag identity/reference IDs, raw paths, exception messages, tokens, recipients, or worker IDs.
- Instrument authoritative services once; do not duplicate metrics in controllers or repositories.
- Wrap scheduled work with `ScheduledJobObserver`, preserve exception behavior, and use injected `Clock`.
- Register executors once, retain bounded queues and context decorators, and reuse stable meter IDs.
- Admin reports answer business questions; Prometheus meters answer operational questions.

- Confirmed paid cancellation uses the refund-request orchestration; initiated cancel remains unpaid-only.
- Require exactly one locked successful Payment, then lock Booking/tickets; checked-in tickets abort.
- Preserve `CUSTOMER_CANCELLATION:{bookingId}` and `SHOW_CANCELLATION:{bookingId}:{showId}`.
- Intent, ticket revocation, seat release, cancellation, and events share one transaction.
- Show cancellation is all-or-nothing for the hall-sized affected set; ambiguity aborts.
- Only `REQUESTED -> APPROVED|REJECTED` is active; repeated same decisions are idempotent.
- All decision/workflow/event times use injected `Clock`.
- Events/audits omit idempotency keys, notes, QR data, and provider payloads.
- Never describe REQUESTED/APPROVED as completed or change Payment before Refund-3.
Refund claims are persisted before execution and attempts are append-only. Retry only stable transient classifications, never unknown outcomes; stale PROCESSING claims move to MANUAL_REVIEW because dispatch cannot be proven absent. Operators must reconcile externally and use manual success only with a verified reference. Never store/log provider payloads, signatures, QR data, credentials, or raw exceptions. All refund timestamps and delays use the injected Spring Clock. Payment REFUNDED requires a full amount/currency match and confirmed Refund SUCCEEDED.
Use per-refund consistency diagnostics before manual financial intervention. A stale recovery always produces manual review, never assumed success. Manual-review success requires independently verified completion and an external reference; exhausted work cannot silently reset attempts. Reports are bounded and currency-separated. Retention is opt-in, oldest-first, bounded, preserves the financial core, and skips active or uncertain records. Global scans and CSV export remain deferred until safe database-first/export infrastructure exists.

## Structured logging policy and runbooks

Event names are stable lowercase dot-separated values. Expected validation/not-found failures do not receive ERROR stack traces; unexpected failures log once at ERROR. Provider failures use bounded categories, not payloads or exception messages. External strings must pass control/newline removal and length bounding. Never log passwords, OTPs, tokens, authorization/cookies, signatures/provider payloads, email bodies/attachments, QR material, or database bind values. Job summaries contain only job/run correlation, outcome, counts and duration.

- **5xx:** search by correlation ID, inspect the unexpected-failure event, complementary audit/persisted work, health and metrics.
- **Slow request:** compare HTTP/business/provider timers, Hikari pending connections, executor/job saturation, and approved PostgreSQL fingerprints.
- **Email failure:** inspect delivery state/retries, bounded SMTP category, executor queue and rejection metrics—never email content.
- **Payment/refund uncertainty:** inspect audit, attempts, provider-operation events and reconciliation/manual review, not raw payloads.
- **Job failure:** search by job-run correlation, check the single summary, failure and last-success metrics, and retryable rows.

## Dashboards, alerts, and SLO operations

Five provisioned dashboards cover application/HTTP, JVM/database/executor capacity, bounded business outcomes, jobs/workers, and payments/refunds. New alerts require a stable existing metric, bounded labels, severity, sustained `for`, dashboard, and runbook. Never add entity/reference variables or financial PromQL reports. Initial targets—99.5% availability, latency once histograms are validated, 98% eligible payment success, 97% notification delivery, and critical jobs within 3× interval—require production baselining and tuning. Review resource, pool, executor, job-duration and throughput trends for capacity. Full thresholds, security boundaries, upgrades and runbooks live in `monitoring/README.md`.
