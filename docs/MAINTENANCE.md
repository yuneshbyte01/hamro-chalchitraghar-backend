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
public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest dto) {
    MovieResponse created = movieService.addMovie(dto);
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

Entity defaults can be set with `@PrePersist`, as seen in `Hall`, `Show`, `Seat`, and `Booking`.

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
