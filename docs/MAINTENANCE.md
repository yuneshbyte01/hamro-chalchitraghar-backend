# Maintenance Guide

This guide describes how to extend and maintain Hamro Chalchitraghar Backend while staying consistent with the current implementation.

## Development Workflow

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
