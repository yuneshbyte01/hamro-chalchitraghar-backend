# Database

Hamro Chitralekha Backend uses PostgreSQL in development and production. The schema is managed by Flyway SQL migrations in `src/main/resources/db/migration`.

Hibernate is configured with `ddl-auto: validate`, so the application validates the schema at startup instead of generating it.

## Database Overview

| Item                 | Implementation                             |
|----------------------|--------------------------------------------|
| Database engine      | PostgreSQL                                 |
| Local Docker version | PostgreSQL 16                              |
| ORM                  | Spring Data JPA / Hibernate                |
| Migration tool       | Flyway                                     |
| Migration location   | `classpath:db/migration`                   |
| Primary key strategy | `BIGSERIAL` database identity columns      |
| Shared audit fields  | `created_at`, `updated_at` on all entities |

## ER Diagram

```mermaid
erDiagram
    USERS ||--o{ BOOKINGS : creates
    USERS ||--o{ PASSWORD_RESET_OTPS : requests
    MOVIES ||--o{ SHOWS : scheduled_for
    HALLS ||--o{ SHOWS : hosts
    HALLS ||--o{ SEAT_TEMPLATES : defines
    SHOWS ||--o{ SEATS : contains
    SHOWS ||--o{ BOOKINGS : booked_for
    BOOKINGS ||--o{ BOOKING_SEATS : includes
    SEATS ||--o{ BOOKING_SEATS : selected_as

    USERS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        varchar name
        varchar email UK
        varchar password
        varchar role
        varchar auth_provider
        varchar google_id
        boolean email_verified
        varchar avatar_url
        boolean enabled
        boolean locked
        integer failed_login_attempts
        timestamp locked_until
        timestamp last_login_at
        timestamp password_changed_at
    }

    PASSWORD_RESET_OTPS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint user_id FK
        varchar otp_hash
        timestamp expires_at
        timestamp used_at
        integer attempt_count
    }

    MOVIES {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        varchar title
        varchar genre
        integer duration_minutes
        varchar language
        varchar description
        varchar poster_url
        date release_date
        varchar status
    }

    HALLS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        varchar name UK
        integer capacity
        varchar layout_ref
        varchar status
    }

    SEAT_TEMPLATES {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint hall_id FK
        varchar row_label
        integer seat_number
        varchar seat_code
        varchar seat_type
        integer position_index
    }

    SHOWS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint movie_id FK
        bigint hall_id FK
        varchar status
        date show_date
        time show_time
        time end_time
    }

    SEATS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint show_id FK
        integer seat_number
        varchar row_label
        varchar seat_code
        varchar seat_type
        numeric price
        varchar seat_status
        integer position_index
        timestamp locked_at
        timestamp lock_expires_at
        bigint locked_by_user_id
    }

    BOOKINGS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint user_id FK
        bigint show_id FK
        timestamp booking_time
        varchar status
    }

    BOOKING_SEATS {
        bigint id PK
        timestamp created_at
        timestamp updated_at
        bigint booking_id FK
        bigint seat_id FK
    }
```

## Tables

### `users`

Stores authentication and authorization accounts.

| Column           | Type           | Nullable | Constraints                                      | Entity field     |
|------------------|----------------|----------|--------------------------------------------------|------------------|
| `id`             | `BIGSERIAL`    | No       | Primary key                                      | `id`             |
| `created_at`     | `TIMESTAMP`    | No       | Set by `GenericEntity`                           | `createdAt`      |
| `updated_at`     | `TIMESTAMP`    | No       | Set by `GenericEntity`                           | `updatedAt`      |
| `name`           | `VARCHAR(255)` | No       | Not blank validation                             | `name`           |
| `email`          | `VARCHAR(255)` | No       | Unique `uk_users_email`, email validation        | `email`          |
| `password`       | `VARCHAR(255)` | Yes      | BCrypt hash for local accounts; null for Google-only accounts | `password`       |
| `role`           | `VARCHAR(255)` | No       | Enum string: `CUSTOMER`, `STAFF`, `ADMIN`        | `role`           |
| `auth_provider`  | `VARCHAR(20)`  | No       | Enum string: `LOCAL`, `GOOGLE`; default `LOCAL`  | `authProvider`   |
| `google_id`      | `VARCHAR(255)` | Yes      | Google account subject identifier                | `googleId`       |
| `email_verified` | `BOOLEAN`      | No       | Default `false`; true for verified Google email  | `emailVerified`  |
| `avatar_url`     | `VARCHAR(500)` | Yes      | Google profile picture URL                       | `avatarUrl`      |
| `enabled`        | `BOOLEAN`      | No       | Default `true`; disabled accounts cannot authenticate | `enabled`        |
| `locked`         | `BOOLEAN`      | No       | Default `false`; true during account lockout     | `locked`         |
| `failed_login_attempts` | `INTEGER` | No      | Default `0`; wrong local password counter        | `failedLoginAttempts` |
| `locked_until`   | `TIMESTAMP`    | Yes      | End of temporary lockout window                  | `lockedUntil`    |
| `last_login_at`  | `TIMESTAMP`    | Yes      | Updated after successful local or Google login   | `lastLoginAt`    |
| `password_changed_at` | `TIMESTAMP` | Yes     | Updated after registration, profile password change, and OTP reset | `passwordChangedAt` |

Authentication provider rules:

- `LOCAL` users authenticate with BCrypt password login and may also be linked to Google by verified email.
- Admin-created internal users are always `LOCAL` and receive a BCrypt password hash at creation time.
- `GOOGLE` users authenticate with Google ID tokens; their password is nullable and password login is rejected with a clean authentication error.
- Account linking stores Google metadata without overwriting an existing local password.
- Local password failures increment `failed_login_attempts`; 5 failures lock the account for 15 minutes.
- Successful local or Google login clears lock state and updates `last_login_at`.
- `password_changed_at` is used to reject JWTs issued before the latest password change.
- Administrative role changes update only `role`; password, provider, Google metadata, login counters, lock expiry, last login, and password change timestamp are not lifecycle-update fields.
- The application protects the last enabled `ADMIN` in service logic before disabling, locking, or changing that account to another role.

### `password_reset_otps`

Stores one-time password reset OTP records. Plain OTP values are never stored; only an SHA-256 hash of the email OTP is persisted.

| Column          | Type           | Nullable | Constraints            | Entity field   |
|-----------------|----------------|----------|------------------------|----------------|
| `id`            | `BIGSERIAL`    | No       | Primary key            | `id`           |
| `created_at`    | `TIMESTAMP`    | No       | Set by `GenericEntity` | `createdAt`    |
| `updated_at`    | `TIMESTAMP`    | No       | Set by `GenericEntity` | `updatedAt`    |
| `user_id`       | `BIGINT`       | No       | FK to `users.id`       | `user`         |
| `otp_hash`      | `VARCHAR(255)` | No       | SHA-256 hash of OTP    | `otpHash`      |
| `expires_at`    | `TIMESTAMP`    | No       | 10-minute OTP expiry   | `expiresAt`    |
| `used_at`       | `TIMESTAMP`    | Yes      | Set when OTP is used   | `usedAt`       |
| `attempt_count` | `INTEGER`      | No       | Defaults to `0`        | `attemptCount` |

OTP lifecycle:

- Forgot password creates a secure random 6-digit numeric OTP, sends the plain OTP by email, and stores only `otp_hash`.
- Creating a new OTP marks previous unused OTPs for the same user as used.
- Reset password hashes the submitted OTP and compares it to the latest unused OTP for the email.
- OTPs are rejected when missing, expired, already used, not latest, or above the configured attempt limit.
- Failed OTP verification increments `attempt_count`; the current limit is configurable through `PASSWORD_RESET_MAX_ATTEMPTS`.
- Successful reset updates the BCrypt password hash and sets `used_at`.

### `movies`

Stores movie catalog metadata.

| Column             | Type           | Nullable | Constraints                                     | Entity field      |
|--------------------|----------------|----------|-------------------------------------------------|-------------------|
| `id`               | `BIGSERIAL`    | No       | Primary key                                     | `id`              |
| `created_at`       | `TIMESTAMP`    | No       | Audit field                                     | `createdAt`       |
| `updated_at`       | `TIMESTAMP`    | No       | Audit field                                     | `updatedAt`       |
| `title`            | `VARCHAR(255)` | No       | Not blank validation                            | `title`           |
| `title_normalized` | `VARCHAR(255)` | No       | Unique with `release_date` for duplicate guard  | `titleNormalized` |
| `genre`            | `VARCHAR(255)` | No       | Not blank validation                            | `genre`           |
| `duration_minutes` | `INTEGER`      | No       | Must be between 1 and 600                       | `durationMinutes` |
| `language`         | `VARCHAR(255)` | No       | Not blank validation                            | `language`        |
| `description`      | `VARCHAR(255)` | No       | Not blank validation                            | `description`     |
| `poster_url`       | `VARCHAR(500)` | No       | Valid `http`/`https` URL                        | `posterUrl`       |
| `release_date`     | `DATE`         | No       | Not null validation                             | `releaseDate`     |
| `status`           | `VARCHAR(255)` | No       | Enum string: `UPCOMING`, `NOW_SHOWING`, `ENDED` | `status`          |

`title_normalized` stores `lower(trim(title))`. The unique index `uk_movies_title_normalized_release_date` enforces case-insensitive duplicate prevention for a movie title on the same release date.

Movies are soft-deleted by changing `status` to `ENDED`; rows are not physically removed and there is no `deleted_at` column. Public movie APIs hide `ENDED` movies, while admin APIs can still query them. Because `shows.movie_id` references `movies.id`, ending a movie is blocked in service code when future active shows exist.

### `halls`

Stores cinema halls.

| Column       | Type           | Nullable | Constraints                                                     | Entity field |
|--------------|----------------|----------|-----------------------------------------------------------------|--------------|
| `id`         | `BIGSERIAL`    | No       | Primary key                                                     | `id`         |
| `created_at` | `TIMESTAMP`    | No       | Audit field                                                     | `createdAt`  |
| `updated_at` | `TIMESTAMP`    | No       | Audit field                                                     | `updatedAt`  |
| `name`       | `VARCHAR(255)` | No       | Unique `uk_halls_name`; normalized unique index `uk_halls_name_normalized`; indexed by `idx_hall_name` | `name`       |
| `capacity`   | `INTEGER`      | No       | Service/API validation: `1` to `1000`                           | `capacity`   |
| `layout_ref` | `VARCHAR(255)` | No       | Required; max `100`; letters, numbers, hyphen, underscore       | `layoutRef`  |
| `status`     | `VARCHAR(255)` | No       | Enum string: `ACTIVE`, `INACTIVE`; indexed by `idx_hall_status` | `status`     |

Hall rows are not physically deleted. Admin delete marks the hall `INACTIVE`; public hall APIs hide inactive halls. Inactivation is blocked in service code when future active shows reference the hall. Once seat templates exist for a hall, `capacity` and `layout_ref` are treated as immutable because generated show seats depend on the original template.

### `seat_templates`

Stores reusable hall seat layouts. A show uses these templates to generate concrete `seats`.

| Column           | Type           | Nullable | Constraints                              | Entity field    |
|------------------|----------------|----------|------------------------------------------|-----------------|
| `id`             | `BIGSERIAL`    | No       | Primary key                              | `id`            |
| `created_at`     | `TIMESTAMP`    | No       | Audit field                              | `createdAt`     |
| `updated_at`     | `TIMESTAMP`    | No       | Audit field                              | `updatedAt`     |
| `hall_id`        | `BIGINT`       | No       | FK to `halls.id`                         | `hall`          |
| `row_label`      | `VARCHAR(255)` | No       | Not blank validation                     | `rowLabel`      |
| `seat_number`    | `INTEGER`      | No       | Positive validation                      | `seatNumber`    |
| `seat_code`      | `VARCHAR(255)` | No       | Generated from row label and seat number | `seatCode`      |
| `seat_type`      | `VARCHAR(255)` | No       | Enum string: `PREMIUM`, `PLATINUM`       | `seatType`      |
| `position_index` | `INTEGER`      | No       | Used for display ordering                | `positionIndex` |

The current seat layout generator creates 188 templates per hall:

| Rows          | Count | Type       |
|---------------|-------|------------|
| `A1` to `A8`  | 8     | `PREMIUM`  |
| `B1` to `J20` | 180   | `PLATINUM` |

Seat template generation currently requires the hall to be `ACTIVE`, to have no existing templates, and to have `capacity = 188`.

Template integrity is also protected by unique constraints on `(hall_id, seat_code)`, `(hall_id, row_label, seat_number)`, and `(hall_id, position_index)`. Concrete show seats have corresponding unique constraints on `(show_id, seat_code)`, `(show_id, row_label, seat_number)`, and `(show_id, position_index)`. These constraints prevent duplicate inventory during concurrent or accidental writes.

### `shows`

Stores scheduled screenings.

| Column       | Type           | Nullable | Constraints                                                   | Entity field |
|--------------|----------------|----------|---------------------------------------------------------------|--------------|
| `id`         | `BIGSERIAL`    | No       | Primary key                                                   | `id`         |
| `created_at` | `TIMESTAMP`    | No       | Audit field                                                   | `createdAt`  |
| `updated_at` | `TIMESTAMP`    | No       | Audit field                                                   | `updatedAt`  |
| `movie_id`   | `BIGINT`       | No       | FK to `movies.id`                                             | `movie`      |
| `hall_id`    | `BIGINT`       | No       | FK to `halls.id`                                              | `hall`       |
| `status`     | `VARCHAR(255)` | No       | Enum string: `SCHEDULED`, `RUNNING`, `COMPLETED`, `CANCELLED` | `status`     |
| `show_date`  | `DATE`         | No       | Must be a future date in request DTO                          | `showDate`   |
| `show_time`  | `TIME`         | No       | Start time                                                    | `showTime`   |
| `end_time`   | `TIME`         | No       | End time                                                      | `endTime`    |

The service prevents overlapping non-cancelled shows in the same hall on the same date and reserves a configurable cleaning buffer (15 minutes by default) on both sides of a candidate interval. Scheduling validation also rejects past starts, non-positive/overnight intervals, and end times outside the configured five-minute tolerance from the movie duration. These remain service-level rules; there is no database exclusion constraint or concurrency-locking redesign.

### `seats`

Stores concrete seats for a specific show.

| Column              | Type               | Nullable | Constraints                                                           | Entity field     |
|---------------------|--------------------|----------|-----------------------------------------------------------------------|------------------|
| `id`                | `BIGSERIAL`        | No       | Primary key                                                           | `id`             |
| `created_at`        | `TIMESTAMP`        | No       | Audit field                                                           | `createdAt`      |
| `updated_at`        | `TIMESTAMP`        | No       | Audit field                                                           | `updatedAt`      |
| `show_id`           | `BIGINT`           | No       | FK to `shows.id`                                                      | `show`           |
| `seat_number`       | `INTEGER`          | No       | Not null validation                                                   | `seatNumber`     |
| `row_label`         | `VARCHAR(255)`     | No       | Not null validation                                                   | `rowLabel`       |
| `seat_code`         | `VARCHAR(255)`     | No       | Not null validation                                                   | `seatCode`       |
| `seat_type`         | `VARCHAR(255)`     | No       | Enum string: `PREMIUM`, `PLATINUM`                                    | `seatType`       |
| `price`             | `NUMERIC(38,2)`    | No       | Positive validation                                                   | `price`          |
| `seat_status`       | `VARCHAR(255)`     | No       | Enum string: `AVAILABLE`, `LOCKED`, `BOOKED`, `RESERVED`, `CANCELLED` | `seatStatus`     |
| `position_index`    | `INTEGER`          | No       | Used for display ordering                                             | `positionIndex`  |
| `locked_at`         | `TIMESTAMP`        | Yes      | Set when held                                                         | `lockedAt`       |
| `lock_expires_at`   | `TIMESTAMP`        | Yes      | Set to 10 minutes after hold                                          | `lockExpiresAt`  |
| `locked_by_user_id` | `BIGINT`           | Yes      | User ID that owns active lock                                         | `lockedByUserId` |

Current generated prices:

| Seat type  | Price   |
|------------|---------|
| `PREMIUM`  | `750.0` |
| `PLATINUM` | `500.0` |

This category pricing is owned by the shared `SeatPricingPolicy` used during show-seat generation.

Concrete show seats are immutable snapshots of the hall templates at show creation time. Cancelling a show retains these records for history; show status controls public availability. `SeatStatus.CANCELLED` remains defined but is currently unused.

Safe show cancellation releases active `LOCKED` seats to `AVAILABLE` and clears `locked_at`, `lock_expires_at`, and `locked_by_user_id`; seat rows are never deleted. Active booking dependency checks include `INITIATED`, `PENDING`, `CONFIRMED`, and `BOOKED`, while `CANCELLED` and `EXPIRED` do not block show updates or cancellation.

### `bookings`

Stores a booking record for one user and one show.

| Column         | Type           | Nullable | Constraints                                                                        | Entity field  |
|----------------|----------------|----------|------------------------------------------------------------------------------------|---------------|
| `id`           | `BIGSERIAL`    | No       | Primary key                                                                        | `id`          |
| `created_at`   | `TIMESTAMP`    | No       | Audit field                                                                        | `createdAt`   |
| `updated_at`   | `TIMESTAMP`    | No       | Audit field                                                                        | `updatedAt`   |
| `user_id`      | `BIGINT`       | No       | FK to `users.id`                                                                   | `user`        |
| `show_id`      | `BIGINT`       | No       | FK to `shows.id`                                                                   | `show`        |
| `booking_time` | `TIMESTAMP`    | No       | Defaults at persist time if not set                                                | `bookingTime` |
| `status`       | `VARCHAR(255)` | No       | Enum string: `INITIATED`, `PENDING`, `CONFIRMED`, `BOOKED`, `CANCELLED`, `EXPIRED` | `status`      |

The current service flow uses `INITIATED`, `CONFIRMED`, and `CANCELLED`.

### `booking_seats`

Join table between bookings and selected seats.

| Column       | Type        | Nullable | Constraints         | Entity field |
|--------------|-------------|----------|---------------------|--------------|
| `id`         | `BIGSERIAL` | No       | Primary key         | `id`         |
| `created_at` | `TIMESTAMP` | No       | Audit field         | `createdAt`  |
| `updated_at` | `TIMESTAMP` | No       | Audit field         | `updatedAt`  |
| `booking_id` | `BIGINT`    | No       | FK to `bookings.id` | `booking`    |
| `seat_id`    | `BIGINT`    | No       | FK to `seats.id`    | `seat`       |

## Constraints

| Name                                  | Table                   | Definition                             |
|---------------------------------------|-------------------------|----------------------------------------|
| `uk_users_email`                      | `users`                 | Unique email                           |
| `uk_movies_title_normalized_release_date` | `movies`            | Unique normalized title and release date |
| `uk_halls_name`                       | `halls`                 | Unique hall name                       |
| `uk_halls_name_normalized`            | `halls`                 | Unique `lower(trim(name))` hall name   |
| `fk_password_reset_otps_user`         | `password_reset_otps`   | `user_id` references `users(id)`       |
| `fk_seat_templates_hall`              | `seat_templates`        | `hall_id` references `halls(id)`       |
| `fk_shows_movie`                      | `shows`                 | `movie_id` references `movies(id)`     |
| `fk_shows_hall`                       | `shows`                 | `hall_id` references `halls(id)`       |
| `fk_seats_show`                       | `seats`                 | `show_id` references `shows(id)`       |
| `fk_bookings_user`                    | `bookings`              | `user_id` references `users(id)`       |
| `fk_bookings_show`                    | `bookings`              | `show_id` references `shows(id)`       |
| `fk_booking_seats_booking`            | `booking_seats`         | `booking_id` references `bookings(id)` |
| `fk_booking_seats_seat`               | `booking_seats`         | `seat_id` references `seats(id)`       |

## Indexes

| Index                                      | Table    | Columns                            |
|--------------------------------------------|----------|------------------------------------|
| `uk_movies_title_normalized_release_date`  | `movies` | `title_normalized`, `release_date` |
| `idx_hall_name`                            | `halls`  | `name`                             |
| `idx_hall_status`                          | `halls`  | `status`                           |

## Relationships

| Relationship           | Type                                 |
|------------------------|--------------------------------------|
| User to bookings       | One user has many bookings           |
| Movie to shows         | One movie has many shows             |
| Hall to shows          | One hall has many shows              |
| Hall to seat templates | One hall has many seat templates     |
| Show to seats          | One show has many generated seats    |
| Show to bookings       | One show has many bookings           |
| Booking to seats       | Many-to-many through `booking_seats` |

## Booking Lifecycle

Bookings have an immutable unique `booking_reference` in `HCG-YYYYMMDD-XXXXXXXX` format, `total_amount NUMERIC(12,2)`, three-letter `currency`, and lifecycle timestamps `expires_at`, `confirmed_at`, `cancelled_at`, and `expired_at`. Each `booking_seats` row stores immutable `unit_price NUMERIC(12,2)` and `(booking_id, seat_id)` is unique. Historical totals are never recalculated from mutable show-seat prices.

The active lifecycle is `INITIATED -> CONFIRMED`, `INITIATED -> CANCELLED`, or `INITIATED -> EXPIRED`. Initiated bookings receive a configured expiry deadline. Lazy reconciliation before reads and confirmation marks stale bookings `EXPIRED`, records `expired_at`, and releases only still-reserved seats without another active booking claim. `PENDING` and `BOOKED` remain enum values for compatibility but are unused by this phase.

`confirmation_source` records `CUSTOMER`, `STAFF`, or `SYSTEM`; current API confirmation writes `CUSTOMER`. The `(status, expires_at)` index supports bounded scheduled cleanup. State-changing lookup uses a pessimistic booking-row lock, while seat locks are acquired in ascending ID order. The unique booking-seat pair remains the database backstop against duplicate claims within one booking.

1. A show is created by admin.
2. Seats are generated from the hall's seat templates.
3. A customer may hold available seats, changing `seats.seat_status` to `LOCKED`.
4. The same customer can create a booking from held or directly available seats.
5. Booking creation sets booking status to `INITIATED`, creates `booking_seats`, and marks seats `RESERVED`.
6. Confirmation sets booking status to `CONFIRMED` and seats to `BOOKED`.
7. Cancellation is allowed only for `INITIATED` bookings before show time and returns seats to `AVAILABLE`.

## Seat Hold Columns

| Column              | Purpose                                                                              |
|---------------------|--------------------------------------------------------------------------------------|
| `seat_status`       | Stores `LOCKED` while a hold is active                                               |
| `locked_at`         | Records when the hold was created or refreshed                                       |
| `lock_expires_at`   | Records when the 10-minute hold expires                                              |
| `locked_by_user_id` | Allows the owning user to convert the hold into a booking while blocking other users |

Seat locking queries use `PESSIMISTIC_WRITE` to prevent concurrent booking updates from claiming the same seat at the same time.

## Flyway Migration Strategy

| Order | Purpose                                         |
|-------|-------------------------------------------------|
| 1     | Creates users                                   |
| 2     | Creates movies                                  |
| 3     | Creates halls and hall indexes                  |
| 4     | Creates reusable hall seat templates            |
| 5     | Creates shows                                   |
| 6     | Creates concrete show seats and hold timestamps |
| 7     | Creates bookings                                |
| 8     | Creates booking-seat join table                 |
| 9     | Adds `locked_by_user_id` to seats               |
| 10    | Creates password reset OTPs                     |
| 11    | Adds Google auth fields to users                |
| 12    | Adds account security fields to users           |
| 13    | Adds movie normalized title uniqueness and expands poster URL length |
| 14    | Adds normalized hall name uniqueness            |

Migration rules:

- Add schema changes as new `V<number>__description.sql` files.
- Do not edit migrations that have already been applied to a shared database.
- Keep entity mappings, DTOs, repositories, and documentation in sync with migrations.
- Run tests after adding migrations.
## Payments

`payments` is the Payment-1 aggregate table. Many attempts may belong to one booking to support future retries. No payment row is currently created automatically or through a public API.

| Column | Type | Rules |
|---|---|---|
| `booking_id` | `BIGINT` | Required FK to `bookings(id)` |
| `payment_reference` | `VARCHAR(50)` | Required, unique, immutable application value in `PAY-YYYYMMDD-XXXXXXXX` format |
| `provider` | `VARCHAR(30)` | `LOCAL`, `ESEWA`, or `KHALTI` |
| `method` | `VARCHAR(30)` | `ONLINE` or `CASH` |
| `status` | `VARCHAR(30)` | `CREATED`, `PENDING`, `SUCCESS`, `FAILED`, `EXPIRED`, `CANCELLED`, or `REFUNDED` |
| `amount` | `NUMERIC(12,2)` | Required and greater than zero |
| `currency` | `VARCHAR(3)` | Required three-character code |
| `provider_transaction_id` | `VARCHAR(255)` | Nullable; repository lookup is scoped by provider |
| `idempotency_key` | `VARCHAR(255)` | Nullable foundation for Payment-2 |
| failure fields | varying | Nullable code/message |
| lifecycle timestamps | `TIMESTAMP` | Initiated/completed/failed/expired/cancelled as applicable |

Indexes cover booking, status, provider/status, creation time, and provider transaction ID. Payment reference uniqueness is enforced by the database. Provider transaction IDs are not yet constrained because provider integration is outside Payment-1.

Payment-2 adds `expires_at`, `(booking_id, idempotency_key)` uniqueness, and indexes on booking/status, status/expiry, and booking/idempotency. The idempotency key remains nullable for Payment-1 legacy/test rows but is mandatory for API initiation. Its exact scope is one booking; booking ownership therefore indirectly scopes it to the customer.
Payment-3 adds `provider_reference`, `provider_status`, `verification_time`, `manual_review_required`, and `manual_review_reason`. A PostgreSQL partial unique index prevents reuse of a non-null `(provider, provider_transaction_id)` pair.
Payment-4 adds `failure_reason` plus operational indexes for manual review, verification time, and amount. The `refunds` table is a persistence skeleton linked many-to-one to payments; no refund service operations, callbacks, or endpoints exist.
## Tickets

The `tickets` table stores one ticket per `booking_seats` row. `ticket_reference` and `booking_seat_id` are both
unique, providing public-reference uniqueness and the final idempotency barrier. Each row links explicitly to its
booking and booking seat and stores `status`, `issued_at`, nullable future lifecycle timestamps/actors, a nullable
revocation reason, and reserved QR version/key identifiers. No QR token or image is stored in Ticket-1.

`TicketStatus` values are `ISSUED`, `CHECKED_IN`, `REVOKED`, and `EXPIRED`; only `ISSUED` is assigned in Ticket-1.

Ticket-2 adds `qr_token_encrypted` (AES-256-GCM versioned ciphertext), `qr_token_hash` (unique SHA-256 hex lookup
hash), `qr_token_version`, `qr_key_id`, and `qr_issued_at`. The random 256-bit raw token is never persisted. PNG
images are derived on demand and are not stored. Future scanners will hash submitted opaque tokens and use the
unique hash index; encrypted ciphertext is never searched.

### `ticket_validations`

Every online scan attempt creates a validation record containing the nullable resolved ticket, required staff/admin
actor, validation time, result, safe reason, and optional device/location/request identifiers. A nullable ticket is
required so unknown opaque tokens can still be audited as `INVALID`. Indexed fields are `ticket_id`,
`validation_time`, and `result`. Results are `SUCCESS`, `ALREADY_USED`, `REVOKED`, `EXPIRED`, `TOO_EARLY`,
`TOO_LATE`, `INVALID`, `SHOW_CANCELLED`, `BOOKING_CANCELLED`, and `SYSTEM_ERROR`.

Ticket-4 adds ticket reissue metadata (`reissued_at`, actor, and reason) and operational indexes. The
`ticket_deliveries` table stores one unique booking/channel delivery with recipient, status, attempt count, safe
failure message, and attempt/sent timestamps. It stores neither SMTP credentials nor PDF bytes. Delivery statuses
are `PENDING`, `SENT`, and `FAILED`; the only current channel is `EMAIL`.
# Notifications

Migration `V26__create_notifications_table.sql` introduces `notifications`. Each row belongs to one
user, one stable event key, and one channel. Notification-1 creates only `IN_APP` rows; `EMAIL` is a
reserved general-notification enum value and remains separate from `ticket_deliveries`.

Columns are `id`, audit timestamps, `user_id`, `type`, `channel`, `event_key`, `title`, `message`,
nullable `payload`, `is_read`, nullable `read_at`, and `occurred_at`. The foreign key references
`users(id)`. `(user_id, event_key, channel)` is unique and is the final concurrent-idempotency
boundary. Blank event keys, titles, and messages are rejected.

Indexes support owner/time lists, owner/read/time lists, type/time filters, and channel/time filters.
Payload uses validated `TEXT` JSON rather than PostgreSQL `JSONB` so the same entity mapping remains
portable to the project's H2 `create-drop` integration tests. It is optional metadata and is never
an authorization source.

Read state is deliberately independent of future delivery state. Notification-1 has no delivery
status, attempts, recipients, failure fields, or retry indexes.
# Notification-2 idempotency

Notification-2 requires no schema change beyond the Notification-1 `notifications` table. The
existing `(user_id, event_key, channel)` unique constraint is the final concurrency guard: one row
is stored per recipient, deterministic business event, and channel. Examples include
`USER_REGISTERED:{userId}`, `BOOKING_CONFIRMED:{bookingId}`,
`PAYMENT_SUCCEEDED:{paymentId}`, `SHOW_CANCELLED:{showId}:{userId}`, and
`TICKET_ISSUED:{bookingId}`. Event keys are internal persistence metadata.

## Notification email deliveries

## Notification-4 persistence

V28 creates `notification_preferences` with unique `(user_id, notification_type, channel)` EMAIL
overrides and adds retention indexes plus `anonymized_at`. Reminder keys use
`SHOW_REMINDER:{bookingId}:{duration}`. Retention anonymizes instead of deleting, preserving event-key
tombstones and foreign-key integrity.


Migration V27 adds `notification_deliveries`, related many-to-one to `notifications`. It snapshots
the recipient and stores channel, `PENDING/PROCESSING/SENT/FAILED/EXHAUSTED/SKIPPED` status,
bounded attempts, retry timestamps, sanitized failure reason, worker claim, template identifier,
subject, and content version. `(notification_id, channel)` is unique. Retry, notification,
recipient, and stale-claim indexes support bounded operational processing. Rows are retained for
audit until a future retention policy is introduced; customer APIs do not expose them.
