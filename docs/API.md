# API Reference

Base URL for local development:

```text
http://localhost:8080
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Standard API Response Format

Successful and error responses use `ApiResponse<T>` except endpoints that intentionally return `204 No Content`.

```json
{
  "success": true,
  "message": "Operation completed",
  "data": {},
  "errors": []
}
```

Validation error example:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": ["email: Invalid email address"]
}
```

## Authentication and Roles

JWT tokens are sent as bearer tokens:

```http
Authorization: Bearer <token>
```

| Route prefix       | Authentication         |
|--------------------|------------------------|
| `/api/auth/**`     | Public                 |
| `/api/public/**`   | Public                 |
| `/api/customer/**` | CUSTOMER, STAFF, ADMIN |
| `/api/staff/**`    | STAFF, ADMIN           |
| `/api/admin/**`    | ADMIN                  |

## Common Error Responses

| Status | Typical message                                              | Cause                                                    |
|--------|--------------------------------------------------------------|----------------------------------------------------------|
| `400`  | `Validation failed`                                          | Bean Validation failure                                  |
| `400`  | `The request body is invalid or cannot be parsed`            | Malformed JSON                                           |
| `400`  | `You cannot disable your own account` or `You cannot lock your own account` | Admin account state self-protection |
| `401`  | `Authentication required`                                    | Missing token on protected endpoint                      |
| `401`  | `Invalid token` or `Token expired`                           | JWT validation failure                                   |
| `401`  | `Account is disabled`                                        | Account is disabled for login or token use               |
| `401`  | `Account is temporarily locked. Please try again later.`      | Account is within the failed-login lockout window        |
| `401`  | `Token is no longer valid after password change`              | JWT was issued before `password_changed_at`              |
| `403`  | `Access denied`                                              | Authenticated role is not allowed                        |
| `404`  | `<Resource> not found with id: <id>`                         | Missing entity                                           |
| `409`  | Conflict-specific message                                    | Seat conflict, hall conflict, or data integrity conflict |
| `500`  | `An unexpected error occurred while processing your request` | Unhandled server error                                   |

## Auth Endpoints

### `POST /api/auth/register`

| Field          | Value                                                                    |
|----------------|--------------------------------------------------------------------------|
| Authentication | Public                                                                   |
| Description    | Registers a new customer account. Passwords are stored as BCrypt hashes. |

Request:

```json
{
  "name": "Aarav Sharma",
  "email": "aarav@example.com",
  "password": "StrongPass123!"
}
```

Validation:

| Field      | Rules                                                                                                                      |
|------------|----------------------------------------------------------------------------------------------------------------------------|
| `name`     | Required, not blank                                                                                                        |
| `email`    | Required, valid email, unique                                                                                              |
| `password` | Required, at least 8 characters, at least one uppercase letter, one lowercase letter, one digit, and one special character |

Response `201`:

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "message": "User registered successfully",
    "email": "aarav@example.com"
  },
  "errors": []
}
```

Errors: `400` validation or duplicate email.

### `POST /api/auth/login`

| Field          | Value                                                                         |
|----------------|-------------------------------------------------------------------------------|
| Authentication | Public                                                                        |
| Description    | Authenticates credentials and returns a JWT containing email and role claims. |

Request:

```json
{
  "email": "aarav@example.com",
  "password": "StrongPass123!"
}
```

Validation:

| Field      | Rules                 |
|------------|-----------------------|
| `email`    | Required, valid email |
| `password` | Required              |

Response `200`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "aarav@example.com",
    "name": "Aarav Sharma",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Security behavior:

- Wrong password increments `failed_login_attempts`.
- After 5 wrong password attempts, the account is locked for 15 minutes.
- Successful login clears failed attempts and updates `last_login_at`.
- Disabled accounts return `Account is disabled`.
- Locked accounts return `Account is temporarily locked. Please try again later.`
- Google-only accounts return `This account uses Google Sign-In.` when password login is attempted.

Errors: `401` invalid credentials, disabled account, locked account, or Google-only account password login.

### `POST /api/auth/google`

| Field          | Value                                                                                                                                             |
|----------------|---------------------------------------------------------------------------------------------------------------------------------------------------|
| Authentication | Public                                                                                                                                            |
| Description    | Verifies a Google ID token with Google's official verifier, links or creates the account, and returns the same JWT response shape as local login. |

Request:

```json
{
  "idToken": "GOOGLE_ID_TOKEN"
}
```

Validation:

| Field     | Rules                                                                                           |
|-----------|-------------------------------------------------------------------------------------------------|
| `idToken` | Required, valid Google ID token, audience must match `GOOGLE_CLIENT_ID`, email must be verified |

Response `200`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "aarav@example.com",
    "name": "Aarav Sharma",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Account handling:

- Existing `LOCAL` account with the same verified email is linked by storing `google_id`, `avatar_url`, and `email_verified=true`; the local password is never overwritten.
- Existing `GOOGLE` account logs in directly and refreshes the avatar URL when it changes.
- New Google users are created with role `CUSTOMER`, `auth_provider=GOOGLE`, verified email, nullable password, and JWT authentication.

Security behavior:

- Successful Google login updates `last_login_at`.
- Failed Google token verification does not increment password failed-login counters.
- Disabled and locked account rules still apply after token verification and account lookup.

Errors: `400` missing token, `401` invalid token, expired token, audience mismatch, unverified Google email, disabled account, or locked account.

### `POST /api/auth/refresh`

| Field          | Value                                              |
|----------------|----------------------------------------------------|
| Authentication | Public                                             |
| Description    | Validates an existing JWT and returns a new token. Tokens issued before the user's latest password change are rejected. |

Request:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

Validation:

| Field   | Rules               |
|---------|---------------------|
| `token` | Required, not blank |

Response `200`:

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "aarav@example.com",
    "name": "Aarav Sharma",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Errors: `401` invalid token, expired token, disabled account, locked account, or token issued before `password_changed_at`.

### `POST /api/auth/forgot-password`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Sends a password reset OTP if the email belongs to an account. The response never reveals whether the email exists. |

Request:

```json
{
  "email": "aarav@example.com"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `email` | Required, valid email |

Response `200`:

```json
{
  "success": true,
  "message": "If an account exists with this email, password reset instructions have been sent.",
  "data": null,
  "errors": []
}
```

### `POST /api/auth/reset-password`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Resets a password using the one-time OTP sent to the account email. |

Request:

```json
{
  "email": "aarav@example.com",
  "otp": "123456",
  "newPassword": "NewStrongPass@123"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `email` | Required, valid email |
| `otp` | Required, 6-digit numeric OTP, latest unused OTP for the email, not expired, within attempt limit |
| `newPassword` | Required, at least 8 characters, at least one uppercase letter, one lowercase letter, one digit, one special character, and different from current password |

Response `200`:

```json
{
  "success": true,
  "message": "Password reset successfully",
  "data": null,
  "errors": []
}
```

Errors: `400` invalid, expired, reused OTP, exceeded OTP attempts, weak password, or same password.

## Public Endpoints

### `GET /api/public/health`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Health check endpoint. |
| Request body | None |
| Validation | None |

Response `200`:

```json
{
  "success": true,
  "message": "Health check successful",
  "data": { "status": "UP" },
  "errors": []
}
```

### `GET /api/public/movies`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists movies with pagination, search, filtering, and sorting. |
| Request body | None |
| Validation | Invalid sort fields, sort direction, status, date format, or reversed release date range return `400`. |

Query parameters:

| Parameter | Default | Description |
| --- | --- | --- |
| `page` | `0` | Zero-based page index |
| `size` | `20` | Number of movies per page |
| `sortBy` | `releaseDate` | One of `id`, `title`, `genre`, `language`, `releaseDate`, `status`, `createdAt`, `updatedAt`, `durationMinutes` |
| `sortDir` | `asc` | `asc` or `desc` |
| `search` | none | Case-insensitive match against `title`, `genre`, or `language` |
| `status` | none | `UPCOMING` or `NOW_SHOWING`; `ENDED` is rejected for public listing |
| `genre` | none | Case-insensitive exact genre filter |
| `language` | none | Case-insensitive exact language filter |
| `releaseDateFrom` | none | Inclusive lower release date bound, `yyyy-MM-dd` |
| `releaseDateTo` | none | Inclusive upper release date bound, `yyyy-MM-dd` |

Example filters:

```http
GET /api/public/movies?search=jatra
GET /api/public/movies?status=NOW_SHOWING&language=Nepali
GET /api/public/movies?releaseDateFrom=2026-01-01&releaseDateTo=2026-12-31
```

Response `200`: `PageResponse<PublicMovieSummaryResponse>`.

```json
{
  "success": true,
  "message": "Movies fetched successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "title": "Jatra",
        "genre": "Comedy",
        "durationMinutes": 125,
        "language": "Nepali",
        "posterUrl": "https://example.com/posters/jatra.jpg",
        "releaseDate": "2026-08-15",
        "status": "NOW_SHOWING"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "errors": []
}
```

Public movie list responses do not expose `description`, `createdAt`, or `updatedAt`. Public lists hide `ENDED` movies by default. Requests with `status=ENDED` return `400` with `Public movie listing does not support status ENDED`.

### `GET /api/public/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one movie by ID. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `PublicMovieDetailResponse` in the standard wrapper. Public detail includes `description`, but does not expose `createdAt` or `updatedAt`. Public detail returns only `UPCOMING` or `NOW_SHOWING` movies.

Errors: `400` invalid ID type, `404` movie not found or movie is `ENDED`.

### `GET /api/public/movies/now-showing`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists movies where `status` is `NOW_SHOWING`, ordered by release date ascending. |
| Request body | None |
| Validation | None |

Response `200`: list of `PublicMovieSummaryResponse`.

### `GET /api/public/movies/upcoming`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists movies where `status` is `UPCOMING`, ordered by release date ascending. |
| Request body | None |
| Validation | None |

Response `200`: list of `PublicMovieSummaryResponse`.

### `GET /api/public/halls`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists active halls using the public summary DTO. INACTIVE halls are hidden. |
| Request body | None |
| Query parameters | `page` default `0`; `size` default `20`; `sortBy` default `name`; `sortDir` default `asc`; optional `search` |
| Search | Case-insensitive match against hall `name` |
| Sorting | `sortBy` must be one of `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, `updatedAt`; `sortDir` must be `asc` or `desc` |

Response `200`:

```json
{
  "success": true,
  "message": "Halls fetched successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Hall A",
        "capacity": 188
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "errors": []
}
```

Response DTO: `PageResponse<PublicHallSummaryResponse>`.

Errors: `400` invalid pagination or sorting parameter.

### `GET /api/public/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one ACTIVE hall by ID. INACTIVE halls return `404`. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `PublicHallDetailResponse`.

Errors: `400` invalid ID type, `404` hall not found or hall is inactive.

### `GET /api/public/halls/active`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists halls where `status` is `ACTIVE` using the public summary DTO. |
| Request body | None |
| Validation | None |

Response `200`: list of `PublicHallSummaryResponse`.

### `GET /api/public/shows`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists publicly visible shows in a `PageResponse`. Cancelled/completed/ended shows and shows linked to a non-now-showing movie or inactive hall are excluded. Running shows remain visible but are not bookable. |
| Request body | None |
| Validation | `page >= 0`, `size >= 1`, valid sorting and ISO `showDate` |

Query parameters: `page` (default `0`), `size` (default `20`), `sortBy` (default `showDate`), `sortDir` (default `asc`), `search`, `movieId`, `hallId`, and `showDate`. Search is case-insensitive across movie title and hall name. Filters can be combined. Allowed sort fields are `showDate`, `showTime`, `endTime`, `status`, `createdAt`, and `updatedAt`; directions are `asc` and `desc`.

Response `200`:

```json
{
  "success": true,
  "message": "Shows fetched successfully",
  "data": {
    "content": [{
      "id": 1,
      "movieId": 1,
      "movieTitle": "Jatra",
      "hallId": 1,
      "hallName": "Hall A",
      "status": "SCHEDULED",
      "showDate": "2026-08-20",
      "showTime": "18:30:00",
      "endTime": "21:00:00"
    }],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  },
  "errors": []
}
```

### `GET /api/public/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one public show by ID. Cancelled/completed shows and shows linked to a non-now-showing movie or inactive hall are treated as not found. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `PublicShowDetailResponse`.

Errors: `400` invalid ID type, `404` show not found.

### `GET /api/public/shows/movie/{movieId}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists publicly visible shows for a movie ID. |
| Request body | None |
| Validation | `movieId` must be numeric |

Response `200`: list of `PublicShowSummaryResponse`.

### `GET /api/public/shows?movieId={movieId}&date={date}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists non-cancelled, non-completed shows for a now-showing movie on a date. |
| Request body | None |
| Validation | `movieId` numeric, `date` parseable as `yyyy-MM-dd` |

Response `200`: list of `PublicShowSummaryResponse`.

Errors: `400` missing or invalid query parameters.

### `GET /api/public/shows/{showId}/seats`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists all seats for a show ordered by `positionIndex`. |
| Request body | None |
| Validation | `showId` must be numeric; show must exist and must not be `CANCELLED` or `COMPLETED` |

This endpoint intentionally returns the complete, non-paginated seat snapshot. Expired locks are normalized to `AVAILABLE` before the response. Internal lock timestamps and ownership are never exposed. Missing, cancelled, and completed shows return `404`.

Response `200`:

```json
{
  "success": true,
  "message": "Seats fetched successfully",
  "data": [
    {
      "id": 10,
      "rowLabel": "A",
      "seatNumber": 1,
      "seatCode": "A1",
      "seatType": "PREMIUM",
      "price": 750.0,
      "positionIndex": 0,
      "seatStatus": "AVAILABLE"
    }
  ],
  "errors": []
}
```

## Customer Endpoints

### `GET /api/customer/profile`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Returns the current authenticated user profile. |
| Request body | None |
| Validation | Valid bearer token |

Response `200`:

```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 1,
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Errors: `401` missing or invalid token.

### `PUT /api/customer/profile`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Updates only the current authenticated user's name. Email and role cannot be changed from this endpoint. |

Request:

```json
{
  "name": "Updated Name"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `name` | Required, not blank, at most 100 characters |

Response `200`:

```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 1,
    "name": "Updated Name",
    "email": "aarav@example.com",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Errors: `400` validation error, `401` missing or invalid token.

### `PUT /api/customer/profile/password`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Changes the current authenticated user's password after verifying the current password. |

Request:

```json
{
  "currentPassword": "OldPass@123",
  "newPassword": "NewStrongPass@123"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `currentPassword` | Required, must match current password |
| `newPassword` | Required, at least 8 characters, at least one uppercase letter, one lowercase letter, one digit, one special character, and different from current password |

Response `200`:

```json
{
  "success": true,
  "message": "Password changed successfully",
  "data": null,
  "errors": []
}
```

Errors: `400` validation error, wrong current password, or same password; `401` missing or invalid token.

### `POST /api/customer/bookings/hold`

Seat holds, booking creation, and booking confirmation require an effectively `SCHEDULED` show whose start remains in the future, movie is `NOW_SHOWING`, and hall is `ACTIVE`. Running, completed, cancelled, stale-started, inactive-hall, and non-now-showing-movie shows return `409`.

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Holds available seats for the current user for 10 minutes. |

Request:

```json
{
  "showId": 1,
  "seatIds": [10, 11]
}
```

Validation:

| Field | Rules |
| --- | --- |
| `showId` | Required |
| `seatIds` | Required, non-empty, no duplicates |

Response `200`:

```json
{
  "success": true,
  "message": "Seats held successfully",
  "data": {
    "message": "Seats held successfully",
    "showId": 1,
    "heldSeatCount": 2,
    "holdExpiresAt": "2026-07-01T18:40:00"
  },
  "errors": []
}
```

Errors: `400` invalid selection, `401` unauthenticated, `409` locked/booked/reserved seat.

### `POST /api/customer/bookings`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Creates an `INITIATED` booking from available seats or seats held by the current user. Seats become `RESERVED`. |

Request:

```json
{
  "showId": 1,
  "seatIds": [10, 11]
}
```

Validation:

| Field | Rules |
| --- | --- |
| `showId` | Required |
| `seatIds` | Required, non-empty, no duplicates |

Response `201`:

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "bookingId": 5,
    "bookingStatus": "INITIATED",
    "showId": 1,
    "movieName": "Jatra",
    "hallName": "Hall A",
    "showDateTime": "2026-08-20T18:30:00",
    "startTime": "18:30",
    "endTime": "21:00",
    "selectedSeats": [],
    "totalAmount": 1250.00,
    "currency": "NPR",
    "bookingTime": "2026-07-01T18:30:00"
  },
  "errors": []
}
```

Errors: `400` duplicate seat IDs, `404` show or seats not found, `409` active booking, locked seat, booked seat, or reserved seat.

### `POST /api/customer/bookings/{bookingId}/confirm`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Confirms an `INITIATED` booking owned by the current user and marks seats `BOOKED`. |
| Request body | None |
| Validation | `bookingId` numeric, booking belongs to current user, booking status is `INITIATED` |

Response `200`: `CustomerBookingDetailResponse` with `bookingStatus` as `CONFIRMED`.

Errors: `400` invalid booking state, `403` booking does not belong to user, `404` booking not found, `409` seats already confirmed elsewhere.

### `GET /api/customer/bookings/my`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Lists bookings for the current user ordered by booking time descending. |
| Request body | None |
| Validation | Valid bearer token |

Response `200`: `PageResponse<CustomerBookingSummaryResponse>`. Query parameters are `page` (default `0`), `size` (default `20`), `sortBy` (default `bookingTime`), `sortDir` (default `desc`), `status`, `showDateFrom`, and `showDateTo`. Only the authenticated customer's bookings are queried; customer identity and audit timestamps are not exposed.

### `GET /api/customer/bookings/{bookingId}`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Fetches one booking. Customers can access only their own bookings; staff and admins can access any booking through the same service rule. |
| Request body | None |
| Validation | `bookingId` numeric |

Response `200`: owner-only `CustomerBookingDetailResponse`.

Errors: `403` booking not accessible, `404` booking not found.

### `POST /api/customer/bookings/{bookingId}/cancel`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Cancels an `INITIATED` booking owned by the current user before show time and releases seats to `AVAILABLE`. |
| Request body | None |
| Validation | `bookingId` numeric, owner only, status must be `INITIATED`, show time must not have passed |

Response `200`: `CustomerBookingDetailResponse` with `bookingStatus` as `CANCELLED`.

Errors: `400` invalid booking state or show already started, `403` owner mismatch, `404` booking not found.

## Staff Endpoints

### `GET /api/staff/bookings/{bookingId}`

| Field | Value |
| --- | --- |
| Authentication | STAFF, ADMIN |
| Description | Fetches a booking by ID for staff workflows. |
| Request body | None |
| Validation | `bookingId` numeric |

Response `200`: `StaffBookingDetailResponse`, including safe customer identity and booking audit timestamps.

### `GET /api/staff/bookings`

Requires `STAFF` or `ADMIN`. Returns `PageResponse<StaffBookingSummaryResponse>`. Supports `page`, `size`, `sortBy`, `sortDir`, `search`, `status`, `showId`, `movieId`, `hallId`, `customerId`, `showDateFrom`, `showDateTo`, `bookingTimeFrom`, and `bookingTimeTo`. Search is case-insensitive across customer name/email, movie title, and hall name.

### `GET /api/admin/bookings/{bookingId}`

Requires authentication and the `ADMIN` role. Returns any booking as `AdminBookingDetailResponse`, including safe customer identity and booking audit timestamps. Customer and staff callers receive `403`; unauthenticated callers receive `401`.

Booking reference lookup is available at `GET /api/customer/bookings/reference/{bookingReference}`, `GET /api/staff/bookings/reference/{bookingReference}`, and `GET /api/admin/bookings/reference/{bookingReference}`. Customer lookup is authenticated and owner-only; a non-owner receives `404`. References use `HCG-YYYYMMDD-XXXXXXXX`. Booking totals and selected-seat prices are immutable NPR snapshots. `INITIATED` bookings expose `expiresAt` and expire after the configured deadline.

Confirmation and cancellation lock the booking row before evaluating state. Repeated confirmation of `CONFIRMED` and repeated cancellation of `CANCELLED` are idempotent successes; incompatible terminal states return `409`/the established invalid-state response. Confirmation passes through `PaymentAuthorizationService`; the current local implementation preserves existing behavior without contacting a payment provider. Confirmed customer cancellation remains unsupported. Initiated cancellation is allowed until the configured pre-show cutoff.

### `GET /api/admin/bookings`

Requires `ADMIN`. Returns `PageResponse<AdminBookingSummaryResponse>` and supports the same filters and safe search fields as the staff list. Allowed booking list sort fields are `id`, `bookingTime`, `status`, `createdAt`, and `updatedAt`; directions are `asc` and `desc`. Invalid statuses, formats, ranges, or sort values return `400`.

Errors: `401` unauthenticated, `403` role not allowed, `404` booking not found.

## Admin Endpoints

### Movies

#### `GET /api/admin/movies`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists movies for admin with pagination, search, filtering, and sorting. Admin lists include all statuses, including `ENDED`. |
| Request body | None |
| Validation | ADMIN token; invalid sort fields, sort direction, status, date format, or reversed release date range return `400` |

Query parameters:

| Parameter | Default | Description |
| --- | --- | --- |
| `page` | `0` | Zero-based page index |
| `size` | `20` | Number of movies per page |
| `sortBy` | `createdAt` | One of `id`, `title`, `genre`, `language`, `releaseDate`, `status`, `createdAt`, `updatedAt`, `durationMinutes` |
| `sortDir` | `desc` | `asc` or `desc` |
| `search` | none | Case-insensitive match against `title`, `genre`, or `language` |
| `status` | none | `UPCOMING`, `NOW_SHOWING`, or `ENDED` |
| `genre` | none | Case-insensitive exact genre filter |
| `language` | none | Case-insensitive exact language filter |
| `releaseDateFrom` | none | Inclusive lower release date bound, `yyyy-MM-dd` |
| `releaseDateTo` | none | Inclusive upper release date bound, `yyyy-MM-dd` |

Response `200`: `PageResponse<AdminMovieSummaryResponse>`.

#### `GET /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one movie by ID for admin. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `AdminMovieDetailResponse`.

Errors: `404` movie not found.

#### `POST /api/admin/movies`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Creates a movie. |

Request:

```json
{
  "title": "Jatra",
  "genre": "Comedy",
  "durationMinutes": 125,
  "language": "Nepali",
  "description": "A Nepali comedy movie about an unexpected chain of events.",
  "posterUrl": "https://example.com/posters/jatra.jpg",
  "releaseDate": "2026-08-15",
  "status": "NOW_SHOWING"
}
```

Validation: `title`, `genre`, `language`, `description`, and `posterUrl` are required; `durationMinutes`, `releaseDate`, and `status` are required; status must be a valid `MovieStatus`.

Additional movie rules:

- Duplicate movies are rejected when `title` matches case-insensitively for the same `releaseDate`.
- `durationMinutes` must be between `1` and `600`.
- `posterUrl` must be a valid `http://` or `https://` URL and at most 500 characters.
- `UPCOMING` movies must have `releaseDate` today or in the future.
- `NOW_SHOWING` and `ENDED` movies must have `releaseDate` today or in the past.

Response `201`: `AdminMovieDetailResponse`.

Errors: `400` validation, `409` duplicate movie or release date/status conflict.

#### `PUT /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Replaces movie fields from `MovieRequest`. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `AdminMovieDetailResponse`.

Status lifecycle:

- `UPCOMING -> NOW_SHOWING`
- `NOW_SHOWING -> ENDED`
- `UPCOMING -> ENDED`

Rejected transitions include `ENDED -> NOW_SHOWING`, `ENDED -> UPCOMING`, and `NOW_SHOWING -> UPCOMING`.

Errors: `400` validation, `404` movie not found, `409` duplicate movie, invalid status transition, release date/status conflict, or future active shows when ending the movie.

#### `DELETE /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-deletes a movie by setting status to `ENDED`. Movie rows are not physically deleted. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

Dependency rule: delete is rejected when the movie has future active shows. Future active shows are `SCHEDULED` or `RUNNING` shows whose show window has not fully passed.

Errors: `404` movie not found, `409` future active shows exist.

### Halls

#### `GET /api/admin/halls`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists all halls for admin using the admin summary DTO. |
| Request body | None |
| Query parameters | `page` default `0`; `size` default `20`; `sortBy` default `createdAt`; `sortDir` default `desc`; optional `search`; optional `status` |
| Search | Case-insensitive match against hall `name` or `layoutRef` |
| Filters | `status=ACTIVE` or `status=INACTIVE` |
| Sorting | `sortBy` must be one of `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, `updatedAt`; `sortDir` must be `asc` or `desc` |

Response `200`: `PageResponse<AdminHallSummaryResponse>`. Admin list responses include `layoutRef` and `status`, but not audit timestamps.

Errors: `400` invalid pagination, sorting, or status parameter.

#### `GET /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one hall by ID for admin. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `AdminHallDetailResponse`.

Errors: `404` hall not found.

#### `GET /api/admin/halls/active`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists active halls. |
| Request body | None |
| Validation | ADMIN token |

Response `200`: list of `AdminHallSummaryResponse`.

#### `POST /api/admin/halls`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Creates a hall. |

Request:

```json
{
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "standard",
  "status": "ACTIVE"
}
```

Validation: `name`, `capacity`, `layoutRef`, and `status` are required; `name` must be unique; status must be `ACTIVE` or `INACTIVE`.

Detailed validation:

- `name` is trimmed and must be unique case-insensitively.
- `capacity` must be between `1` and `1000`.
- `layoutRef` is trimmed, required, at most `100` characters, and may contain only letters, numbers, hyphen, and underscore.
- `status` may be `ACTIVE` or `INACTIVE`; the requested initial status is respected.

Response `201`: `AdminHallDetailResponse`.

Errors: `400` validation failure, `409` duplicate hall name.

#### `PUT /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Replaces hall fields from `HallRequest`. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `AdminHallDetailResponse`.

Lifecycle rule: status may move `ACTIVE -> INACTIVE` or `INACTIVE -> ACTIVE`.

Dependency rules:

- Inactivation through update is rejected when the hall has future active shows.
- Future active shows are `SCHEDULED` or `RUNNING` shows whose show window has not fully passed.
- Once seat templates exist for a hall, `capacity` and `layoutRef` cannot be changed. `name` and `status` may still be updated subject to other rules.

Errors: `400` validation failure, `404` hall not found, `409` duplicate hall name, invalid lifecycle transition, future active shows, or locked seat layout fields.

#### `DELETE /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-deletes a hall by setting status to `INACTIVE`. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

Dependency rule: delete is a soft inactivation and is rejected when the hall has future active shows.

Errors: `404` hall not found, `409` future active shows exist.

#### `GET /api/admin/halls/{hallId}/seat-layout`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Returns generated seat templates with optional search, filtering, and sorting. Statistics describe the returned templates. |
| Request body | None |
| Validation | `hallId` must be numeric; hall and generated layout must exist |

Optional query parameters:

| Parameter | Behavior |
| --- | --- |
| `search` | Case-insensitive partial match against `seatCode` or `rowLabel` |
| `seatType` | Exact enum filter: `PREMIUM` or `PLATINUM` |
| `row` | Case-insensitive exact row-label filter |
| `sortBy` | `positionIndex`, `rowLabel`, `seatNumber`, `seatCode`, or `seatType`; defaults to `positionIndex` |
| `sortDir` | `asc` or `desc`; defaults to `asc` |

Search and filters can be combined. The `totalSeats`, category counts, `rows`, and `templates` fields all describe the filtered result.

Response `200`:

```json
{
  "success": true,
  "message": "Seat layout fetched successfully",
  "data": {
    "hallId": 1,
    "hallName": "Hall A",
    "capacity": 188,
    "totalSeats": 188,
    "premiumSeats": 8,
    "platinumSeats": 180,
    "rows": ["A", "B", "C", "D", "E", "F", "G", "H", "I", "J"],
    "templates": [
      {
        "id": 1,
        "rowLabel": "A",
        "seatNumber": 1,
        "seatCode": "A1",
        "seatType": "PREMIUM",
        "positionIndex": 0
      }
    ]
  },
  "errors": []
}
```

Errors: `400` invalid `seatType`, `sortBy`, or `sortDir`; `404` hall not found or hall has no generated seat layout.

#### `POST /api/admin/halls/{hallId}/seat-layout`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Generates seat templates for an active hall. The current generator creates 188 templates. |
| Request body | None |
| Validation | `hallId` numeric; hall must be `ACTIVE`; layout must not already exist; hall capacity must be `188` |

Before persistence, the complete preset is validated: rows are one or two uppercase letters `A-Z`; seat numbers are `1` to `100`, unique and sequential within each row; `seatCode` equals `rowLabel + seatNumber`; positions start at `0` and are unique and gapless; categories are `PREMIUM` or `PLATINUM`; and template count equals hall capacity. A validation or capacity mismatch returns `409` without partial generation.

Response `200`:

```json
{
  "success": true,
  "message": "Seat layout generated successfully",
  "data": null,
  "errors": []
}
```

Errors: `404` hall not found, `409` inactive hall, layout already exists, or capacity does not match the fixed 188-seat generator.

#### `POST /api/admin/halls/{hallId}/seat-layout/regenerate`

Regenerates the same fixed validated preset for an `ACTIVE` hall. An existing layout is required, capacity must remain supported, and no show of any status may reference the hall. The operation deletes and recreates templates transactionally and returns `AdminSeatLayoutResponse`.

Errors: `404` hall or existing layout not found; `409` inactive/unsupported hall or any dependent show exists.

### Shows

#### `GET /api/admin/shows`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists all shows for admin in a `PageResponse`. |
| Request body | None |
| Validation | ADMIN token, `page >= 0`, `size >= 1`, valid status/sorting and ISO `showDate` |

Query parameters: `page` (default `0`), `size` (default `20`), `sortBy` (default `showDate`), `sortDir` (default `asc`), `search`, `movieId`, `hallId`, `status`, and `showDate`. Search is case-insensitive across movie title and hall name. Filters can be combined. Status supports `SCHEDULED`, `RUNNING`, `COMPLETED`, and `CANCELLED`. Allowed sort fields are `showDate`, `showTime`, `endTime`, `status`, `createdAt`, and `updatedAt`; directions are `asc` and `desc`.

Response `200`: `PageResponse<AdminShowSummaryResponse>`. All show statuses and historical movie/hall associations are visible.

#### `GET /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one show by ID for administration, including scheduled, running, completed, and cancelled shows. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `AdminShowDetailResponse`.

Errors: `404` show not found.

#### `POST /api/admin/shows`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Creates a show and generates seats from the hall seat templates. |

Request:

```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-08-20",
  "showTime": "18:30:00",
  "endTime": "21:00:00"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `movieId` | Required; movie must exist and have status `NOW_SHOWING` |
| `hallId` | Required; hall must exist and not be `INACTIVE` |
| `showDate` | Required; must be today or later |
| `showTime` | Required; for today, must be later than the current time |
| `endTime` | Required; must be after `showTime`; overnight and zero-length shows are rejected |
| Duration | End time must be within five minutes of `showTime + movie.durationMinutes` |
| Schedule | Must not overlap another non-cancelled show or its configured 15-minute cleaning buffer in the same hall/date |
| Seat templates | The hall must already have seat templates |

Response `201`: `AdminShowDetailResponse`.

Errors: `400` validation, `404` movie/hall/seat template not found, `409` inactive hall, non-now-showing movie, or overlapping show.

#### `PUT /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Updates a show after validating movie status, hall status, and schedule conflicts. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `AdminShowDetailResponse`.

Errors: `400` validation, `404` show/movie/hall not found, `409` schedule conflict or invalid movie/hall status.

Only effectively `SCHEDULED` shows can be edited through this endpoint. Movie, hall, date, start, and end changes are blocked while an active seat hold or active booking exists. Active bookings are `INITIATED`, `PENDING`, `CONFIRMED`, or `BOOKED`; cancelled/expired bookings do not block updates. `RUNNING` shows permit status changes only; `COMPLETED` and `CANCELLED` shows are terminal and cannot be edited.

#### `PATCH /api/admin/shows/{id}/status`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Applies a validated show lifecycle transition. |
| Validation | `id` numeric and `status` required |

Request:

```json
{ "status": "RUNNING" }
```

Allowed transitions are `SCHEDULED -> RUNNING`, `SCHEDULED -> CANCELLED`, `RUNNING -> COMPLETED`, and `RUNNING -> CANCELLED`. Completed and cancelled shows cannot transition or reopen.

Response `200`: `AdminShowDetailResponse`.

Errors: `400` malformed/missing status, `404` show not found, `409` invalid transition.

#### `DELETE /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-cancels a scheduled show. Active bookings block cancellation; running/completed shows cannot be cancelled. Repeated cancellation is idempotent. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

Successful cancellation releases active seat locks to `AVAILABLE` and clears lock timestamps/ownership. Concrete seat rows remain as historical snapshots. No refund, notification, or automatic booking cancellation is performed.

Errors: `404` show not found.

### Users

#### `GET /api/admin/users`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists users using paginated `AdminUserSummaryResponse` results. Passwords, Google subject IDs, and OTP data are never returned. |
| Request body | None |
| Validation | ADMIN token; invalid sort fields, sort direction, or enum filters return `400` |

Query parameters:

| Parameter | Default | Description |
| --- | --- | --- |
| `page` | `0` | Zero-based page index |
| `size` | `20` | Number of users per page |
| `sortBy` | `createdAt` | One of `id`, `name`, `email`, `role`, `enabled`, `locked`, `authProvider`, `createdAt`, `updatedAt`, `lastLoginAt` |
| `sortDir` | `desc` | `asc` or `desc` |
| `search` | none | Case-insensitive match against `name` or `email` |
| `role` | none | `CUSTOMER`, `STAFF`, or `ADMIN` |
| `authProvider` | none | `LOCAL` or `GOOGLE` |
| `enabled` | none | `true` or `false` |
| `locked` | none | `true` or `false` |
| `emailVerified` | none | `true` or `false` |

Example filters:

```http
GET /api/admin/users?search=ram&role=CUSTOMER&enabled=true
GET /api/admin/users?authProvider=GOOGLE&sortBy=lastLoginAt&sortDir=desc
```

Response `200`:

```json
{
  "success": true,
  "message": "Users fetched successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Aarav Sharma",
        "email": "aarav@example.com",
        "role": "CUSTOMER",
        "enabled": true,
        "locked": false
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  },
  "errors": []
}
```

Errors: `400` invalid `sortBy`, `sortDir`, `role`, or `authProvider`; `401` unauthenticated; `403` non-admin role.

#### `POST /api/admin/users`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Creates an internal `LOCAL` user as `CUSTOMER`, `STAFF`, or `ADMIN`. |

Request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "StrongPass@123",
  "role": "STAFF"
}
```

Validation:

| Field | Rules |
| --- | --- |
| `name` | Required, not blank |
| `email` | Required, valid email, unique |
| `password` | Required, at least 8 characters, strong password rules |
| `role` | Required; one of `CUSTOMER`, `STAFF`, `ADMIN` |

Created user defaults: `authProvider=LOCAL`, `emailVerified=false`, `enabled=true`, `locked=false`, `failedLoginAttempts=0`, `passwordChangedAt=now`, and `lastLoginAt=null`.

Response `201`: `AdminUserDetailResponse`.

Errors: `400` validation failure, duplicate email, or invalid role; `401` unauthenticated; `403` non-admin role.

#### `GET /api/admin/users/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one user using `AdminUserDetailResponse`. Passwords, Google subject IDs, internal hashes, and OTP data are never returned. |
| Request body | None |
| Validation | `id` numeric |

Response `200`:

```json
{
  "success": true,
  "message": "User fetched successfully",
  "data": {
    "id": 1,
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "role": "CUSTOMER",
    "authProvider": "LOCAL",
    "emailVerified": false,
    "enabled": true,
    "locked": false,
    "failedLoginAttempts": 0,
    "lockedUntil": null,
    "lastLoginAt": "2026-07-02T10:15:30",
    "passwordChangedAt": "2026-07-01T09:00:00",
    "createdAt": "2026-07-01T09:00:00",
    "updatedAt": "2026-07-02T10:15:30"
  },
  "errors": []
}
```

Errors: `404` user not found.

#### `PUT /api/admin/users/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Updates administrative lifecycle fields for one user. |

Request:

```json
{
  "name": "John Doe",
  "role": "ADMIN",
  "enabled": true,
  "emailVerified": false
}
```

Allowed fields are `name`, `role`, `enabled`, and `emailVerified`. Omitted fields are left unchanged.

Forbidden through this endpoint: `password`, `authProvider`, `googleId`, `avatarUrl`, `failedLoginAttempts`, `lockedUntil`, `lastLoginAt`, and `passwordChangedAt`.

Role management rules:

- Role must be one of `CUSTOMER`, `STAFF`, or `ADMIN`.
- Admins cannot remove their own `ADMIN` role.
- The last enabled admin cannot be changed to another role.
- The last enabled admin cannot be disabled.

Response `200`: `AdminUserDetailResponse`.

Errors: `400` invalid role, self-demotion, self-disable, or last-admin protection; `401` unauthenticated; `403` non-admin role; `404` user not found.

#### `PUT /api/admin/users/{id}/enable`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Manually enables a user account by setting `enabled=true`. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `AdminUserDetailResponse` with `enabled=true`.

Errors: `401` unauthenticated, `403` non-admin role, `404` user not found.

#### `PUT /api/admin/users/{id}/disable`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Manually disables a user account by setting `enabled=false`. Disabled users cannot log in, refresh tokens, or use existing JWTs for protected endpoints. |
| Request body | None |
| Validation | `id` numeric; target must not be the current admin |

Response `200`: `AdminUserDetailResponse` with `enabled=false`.

Errors: `400` admin attempted to disable self, `401` unauthenticated, `403` non-admin role, `404` user not found.

#### `PUT /api/admin/users/{id}/lock`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Manually locks a user account by setting `locked=true` and `lockedUntil=null`. |
| Request body | None |
| Validation | `id` numeric; target must not be the current admin |

Response `200`: `AdminUserDetailResponse` with `locked=true` and `lockedUntil=null`.

Errors: `400` admin attempted to lock self, `401` unauthenticated, `403` non-admin role, `404` user not found.

#### `PUT /api/admin/users/{id}/unlock`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Unlocks a user account by setting `locked=false`, clearing `lockedUntil`, and resetting `failedLoginAttempts=0`. |
| Request body | None |
| Validation | `id` numeric |

Response `200`:

```json
{
  "success": true,
  "message": "User unlocked successfully",
  "data": {
    "id": 1,
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "role": "CUSTOMER",
    "authProvider": "LOCAL",
    "emailVerified": false,
    "enabled": true,
    "locked": false,
    "failedLoginAttempts": 0,
    "lockedUntil": null,
    "lastLoginAt": "2026-07-02T10:15:30",
    "passwordChangedAt": "2026-07-01T09:00:00",
    "createdAt": "2026-07-01T09:00:00",
    "updatedAt": "2026-07-02T10:15:30"
  },
  "errors": []
}
```

Errors: `401` unauthenticated, `403` non-admin role, `404` user not found.

Manual account state notes:

- Manual disable is controlled by `enabled=false` and blocks login plus existing JWT authorization through the JWT filter.
- Manual lock is controlled by `locked=true` and `lockedUntil=null`, so it does not expire automatically.
- Auth-5 automatic lockout uses `locked=true` with a future `lockedUntil`; unlock clears both manual and automatic lock state and resets failed login attempts.
- Internal user creation always creates `LOCAL` accounts with BCrypt password hashes and no Google metadata.

User response separation:

- `UserResponse` is used for the authenticated customer profile and contains only `id`, `name`, `email`, and `role`.
- `AdminUserSummaryResponse` is used for admin user lists and adds `enabled` and `locked`.
- `AdminUserDetailResponse` is used for admin user detail and adds account status, auth provider, login metadata, and audit timestamps.
- `UserMapper` converts `User` entities to each safe response shape.
- `PageResponse<T>` wraps paginated list content with `page`, `size`, `totalElements`, `totalPages`, and `last`.

## Response DTO Summary

| DTO | Fields |
| --- | --- |
| `PublicMovieSummaryResponse` | `id`, `title`, `genre`, `durationMinutes`, `language`, `posterUrl`, `releaseDate`, `status` |
| `PublicMovieDetailResponse` | `id`, `title`, `genre`, `durationMinutes`, `language`, `description`, `posterUrl`, `releaseDate`, `status` |
| `AdminMovieSummaryResponse` | `id`, `title`, `genre`, `language`, `releaseDate`, `status` |
| `AdminMovieDetailResponse` | `id`, `title`, `genre`, `durationMinutes`, `language`, `description`, `posterUrl`, `releaseDate`, `status`, `createdAt`, `updatedAt` |
| `MovieResponse` | Legacy movie response retained for compatibility; new audience-specific contracts use public/admin movie DTOs |
| `PublicHallSummaryResponse` | `id`, `name`, `capacity` |
| `PublicHallDetailResponse` | `id`, `name`, `capacity`, `status` |
| `AdminHallSummaryResponse` | `id`, `name`, `capacity`, `layoutRef`, `status` |
| `AdminHallDetailResponse` | `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, `updatedAt` |
| `PublicShowSummaryResponse` | `id`, `movieId`, `movieTitle`, `hallId`, `hallName`, `status`, `showDate`, `showTime`, `endTime` |
| `PublicShowDetailResponse` | `id`, public `movie`, public `hall`, `status`, `showDate`, `showTime`, `endTime` |
| `AdminShowSummaryResponse` | `id`, `movieId`, `movieTitle`, `hallId`, `hallName`, `status`, `showDate`, `showTime`, `endTime` |
| `AdminShowDetailResponse` | `id`, admin `movie`, admin `hall`, `status`, `showDate`, `showTime`, `endTime`, `createdAt`, `updatedAt` |
| `SeatResponse` | `id`, `rowLabel`, `seatNumber`, `seatCode`, `seatType`, `price`, `positionIndex`, `seatStatus` |
| `CustomerBookingSummaryResponse` | `bookingId`, `bookingReference`, `bookingStatus`, show fields, ordered `selectedSeatCodes`, `totalAmount`, `currency`, `bookingTime`, `expiresAt` |
| `CustomerBookingDetailResponse` | Summary fields plus `startTime`, `endTime`, and `selectedSeats`; excludes customer identity and audit fields |
| `StaffBookingDetailResponse` | Booking detail plus `customerId`, `customerName`, `customerEmail`, `createdAt`, and `updatedAt` |
| `AdminBookingDetailResponse` | Separate admin contract containing the same safe operational fields as the staff detail contract |
| `StaffBookingSummaryResponse` | Operational booking/customer/show fields plus `bookingReference`, ordered `selectedSeatCodes`, stored `totalAmount`, `currency`, `bookingTime`, and `expiresAt` |
| `AdminBookingSummaryResponse` | Separate admin summary contract with the same current safe operational fields |
| `UserResponse` | `id`, `name`, `email`, `role` |
| `AdminUserSummaryResponse` | `id`, `name`, `email`, `role`, `enabled`, `locked` |
| `AdminUserDetailResponse` | `id`, `name`, `email`, `role`, `authProvider`, `emailVerified`, `enabled`, `locked`, `failedLoginAttempts`, `lockedUntil`, `lastLoginAt`, `passwordChangedAt`, `createdAt`, `updatedAt` |
| `PageResponse<T>` | `content`, `page`, `size`, `totalElements`, `totalPages`, `last` |
## Payment-1 Read-Only APIs

The Payment-1 read model exposes persisted payment history. Payment-2 additionally supports safe initiation and local development processing, but does not integrate any external provider, verify gateway payments, collect cash, refund, or reconcile. Payment references are generated by the server in `PAY-YYYYMMDD-XXXXXXXX` format.

All responses use `ApiResponse<T>`. Customer routes require an authenticated owner; unknown and non-owned references both return `404`. Staff routes allow `STAFF` and `ADMIN`; admin routes allow `ADMIN` only.

| Audience | Method and path | Response data |
|---|---|---|
| Customer | `GET /api/customer/payments` | `List<CustomerPaymentSummaryResponse>` for the current user, newest first |
| Customer | `GET /api/customer/payments/{paymentReference}` | `CustomerPaymentDetailResponse` |
| Customer | `GET /api/customer/bookings/{bookingReference}/payments` | Owner-only payment history, newest first |
| Staff | `GET /api/staff/payments/{paymentReference}` | `StaffPaymentDetailResponse` |
| Staff | `GET /api/staff/bookings/{bookingReference}/payments` | `List<StaffPaymentSummaryResponse>` |
| Admin | `GET /api/admin/payments/{paymentReference}` | `AdminPaymentDetailResponse` |
| Admin | `GET /api/admin/bookings/{bookingReference}/payments` | `List<AdminPaymentSummaryResponse>` |

Customer DTOs omit customer identity, internal IDs, internal failure codes, and audit timestamps. Staff/admin DTOs include safe customer identity and operational failure/audit fields. No DTO exposes credentials, authentication data, or raw provider payloads.

### Payment-2 customer mutations

`POST /api/customer/bookings/{bookingReference}/payments` initiates an owned `INITIATED` booking payment. It requires `Idempotency-Key` (trimmed, non-blank, at most 255 characters) and accepts only `provider` and `method`. Amount, currency, customer, and booking are always loaded from server state. Idempotency is scoped to booking plus key: the same key and parameters return the existing payment; parameter reuse conflicts. Only one `CREATED`/`PENDING` attempt may exist, and a successful payment prevents another attempt.

`POST /api/customer/payments/{paymentReference}/cancel` cancels an owned active attempt. Repeating cancellation is idempotent; other terminal states conflict. It does not cancel the booking.

When `PAYMENT_LOCAL_ENABLED=true`, `POST /api/customer/payments/{paymentReference}/process` accepts `{ "result": "SUCCESS" }` or `FAILED` for owned LOCAL payments. This simulator is intended only for development/tests and is absent by default in production. Success does not confirm the booking.

Payment attempts expire after `PAYMENT_ATTEMPT_EXPIRATION_MINUTES` (default 10). Customer-submitted unknown amount/currency fields are ignored by the current Jackson configuration and never used.
## eSewa ePay v2 sandbox flow

Customers initiate an ESEWA/ONLINE attempt with `POST /api/customer/bookings/{bookingReference}/payments`. The backend returns a signed form payload; the frontend must POST every returned form field to `paymentUrl`. After eSewa redirects to the frontend success route with Base64 `data`, the frontend calls public `POST /api/payments/esewa/verify`. The backend verifies the response signature and independently checks eSewa status before changing state. `POST /api/admin/payments/{paymentReference}/reconcile` recovers missed redirects. Verified success confirms an eligible booking and books its seats; late success remains financially successful but is flagged for manual review.
## Payment operations

Admins can page and filter payments with `GET /api/admin/payments`, inspect `GET /api/admin/payments/manual-review`, resolve review items with `POST /api/admin/payments/{paymentReference}/resolve?resolution=CLEAR|KEEP|NO_REFUND_REQUIRED`, view aggregate metrics at `GET /api/admin/payments/statistics`, and inspect detected inconsistencies at `GET /api/admin/payments/consistency`. Staff remains read-only. Scheduled reconciliation and expiry have no public endpoints.
## Ticket APIs

Tickets are issued transactionally only when a payment-driven booking confirmation has changed the booking to
`CONFIRMED` and every selected seat to `BOOKED`. The model creates exactly one `ISSUED` ticket per `BookingSeat`;
repeated confirmation or payment finalization reuses existing tickets. Ticket references use
`TKT-YYYYMMDD-XXXXXXXX`. QR images/tokens, scanning, check-in, revocation, PDF, and email delivery are deferred.

- `GET /api/customer/tickets` lists the authenticated user's tickets newest first.
- `GET /api/customer/tickets/{ticketReference}` is owner-only; unknown/non-owned references return `404`.
- `GET /api/customer/bookings/{bookingReference}/tickets` lists an owned booking's tickets by seat position.
- `GET /api/staff/tickets/{ticketReference}` and `GET /api/staff/bookings/{bookingReference}/tickets` are read-only for `STAFF`/`ADMIN`.
- `GET /api/admin/tickets/{ticketReference}` and `GET /api/admin/bookings/{bookingReference}/tickets` are read-only for `ADMIN`.

Each ticket receives a 256-bit opaque QR token during first issuance. The QR contains only that token—no ticket,
booking, customer, seat, or payment data. `GET /api/customer/tickets/{ticketReference}/qr` is owner-only and returns
an on-demand `image/png` with private/no-store caching. `GET /api/customer/tickets/{ticketReference}/qr-data`
returns only the ticket reference, QR version, and QR issuance time. Neither endpoint exposes plaintext tokens,
hashes, ciphertext, IVs, or key material. QR retrieval itself never performs check-in.

### Staff ticket admission

`POST /api/staff/tickets/scan` accepts only `{ "qrToken": "<opaque token>" }` and requires `STAFF` or `ADMIN`.
Optional `X-Device-ID`, `X-Location`, and `X-Request-ID` headers are retained in validation history. The server
hashes the token, locks the ticket row, checks ticket/booking/show state and the configured admission window, then
atomically records either `SUCCESS` or a rejection result. A successful scan changes `ISSUED` to `CHECKED_IN`;
repeated scans return `ALREADY_USED` and never admit again. Expected validation failures return `200` with
`admitted=false` and a result/reason so admission clients can distinguish operational outcomes.

Admission is permitted from `showStart - TICKET_ENTRY_WINDOW_MINUTES` through
`showEnd + TICKET_POST_SHOW_GRACE_MINUTES`, inclusive. Customer detail exposes only `checkedIn` and `checkedInAt`.
Staff/admin ticket detail includes newest-first validation history.

## Ticket completion operations

- `POST /api/admin/tickets/{ticketReference}/revoke` requires an admin and a non-blank reason. It atomically changes
  `ISSUED` to `REVOKED`; repeats are idempotent, while checked-in/expired tickets conflict.
- `POST /api/admin/tickets/{ticketReference}/reissue` rotates only an `ISSUED` ticket's encrypted opaque token and
  hash, increments its QR version, and preserves its ticket reference. The previous QR stops resolving immediately.
- `GET /api/customer/tickets/{ticketReference}/pdf` downloads one owner-only PDF.
- `GET /api/customer/bookings/{bookingReference}/tickets/pdf` downloads an owner-only, seat-ordered multi-page bundle.
- Customer/staff/admin ticket list routes are paginated and safely sorted. Admin routes also expose `metrics`,
  `validation-summary`, and `inconsistencies` operational views.

Issued tickets expire after the show-end grace window. Show cancellation revokes issued tickets through one shared
orchestration path; checked-in tickets cause an operational conflict rather than silent revocation. PDFs are
generated in memory and include the QR image but never token text, ciphertext, hashes, credentials, or database IDs.
