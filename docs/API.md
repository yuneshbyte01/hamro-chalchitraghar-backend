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
| Description | Lists all shows. |
| Request body | None |
| Validation | None |

Response `200`:

```json
{
  "success": true,
  "message": "Shows fetched successfully",
  "data": [
    {
      "id": 1,
      "movie": { "id": 1, "title": "Jatra", "status": "NOW_SHOWING" },
      "hall": { "id": 1, "name": "Hall A", "status": "ACTIVE" },
      "status": "SCHEDULED",
      "showDate": "2026-08-20",
      "showTime": "18:30:00",
      "endTime": "21:00:00",
      "createdAt": "2026-07-01T10:00:00",
      "updatedAt": "2026-07-01T10:00:00"
    }
  ],
  "errors": []
}
```

### `GET /api/public/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one active public show by ID. Cancelled, completed, or non-now-showing movie shows are treated as not found. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `ShowResponse`.

Errors: `400` invalid ID type, `404` show not found.

### `GET /api/public/shows/movie/{movieId}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists shows for a movie ID. |
| Request body | None |
| Validation | `movieId` must be numeric |

Response `200`: list of `ShowResponse`.

### `GET /api/public/shows?movieId={movieId}&date={date}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists non-cancelled, non-completed shows for a now-showing movie on a date. |
| Request body | None |
| Validation | `movieId` numeric, `date` parseable as `yyyy-MM-dd` |

Response `200`: list of `ShowResponse`.

Errors: `400` missing or invalid query parameters.

### `GET /api/public/shows/{showId}/seats`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists all seats for a show ordered by `positionIndex`. |
| Request body | None |
| Validation | `showId` must be numeric |

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
    "totalPrice": 1250.0,
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

Response `200`: `BookingResponse` with `bookingStatus` as `CONFIRMED`.

Errors: `400` invalid booking state, `403` booking does not belong to user, `404` booking not found, `409` seats already confirmed elsewhere.

### `GET /api/customer/bookings/my`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Lists bookings for the current user ordered by booking time descending. |
| Request body | None |
| Validation | Valid bearer token |

Response `200`: list of `BookingResponse`.

### `GET /api/customer/bookings/{bookingId}`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Fetches one booking. Customers can access only their own bookings; staff and admins can access any booking through the same service rule. |
| Request body | None |
| Validation | `bookingId` numeric |

Response `200`: `BookingResponse`.

Errors: `403` booking not accessible, `404` booking not found.

### `POST /api/customer/bookings/{bookingId}/cancel`

| Field | Value |
| --- | --- |
| Authentication | CUSTOMER, STAFF, ADMIN |
| Description | Cancels an `INITIATED` booking owned by the current user before show time and releases seats to `AVAILABLE`. |
| Request body | None |
| Validation | `bookingId` numeric, owner only, status must be `INITIATED`, show time must not have passed |

Response `200`: `BookingResponse` with `bookingStatus` as `CANCELLED`.

Errors: `400` invalid booking state or show already started, `403` owner mismatch, `404` booking not found.

## Staff Endpoints

### `GET /api/staff/bookings/{bookingId}`

| Field | Value |
| --- | --- |
| Authentication | STAFF, ADMIN |
| Description | Fetches a booking by ID for staff workflows. |
| Request body | None |
| Validation | `bookingId` numeric |

Response `200`: `BookingResponse`.

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

Errors: `400` validation failure, `404` hall not found, `409` duplicate hall name or invalid lifecycle transition.

#### `DELETE /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-deletes a hall by setting status to `INACTIVE`. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

Errors: `404` hall not found.

#### `POST /api/admin/halls/{hallId}/seat-layout`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Generates seat templates for the hall. The current generator creates 188 templates. |
| Request body | None |
| Validation | `hallId` numeric; layout must not already exist |

Response `200`:

```json
{
  "success": true,
  "message": "Seat layout generated successfully",
  "data": null,
  "errors": []
}
```

Errors: `400` layout already exists, `404` hall not found.

### Shows

#### `GET /api/admin/shows`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists all shows for admin. |
| Request body | None |
| Validation | ADMIN token |

Response `200`: list of `ShowResponse`.

#### `GET /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one show by ID using the same service rules as public show lookup. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `ShowResponse`.

Errors: `404` show not found, cancelled, completed, or linked to a non-now-showing movie.

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
| `showDate` | Required; must be in the future |
| `showTime` | Required |
| `endTime` | Required |
| Schedule | Must not overlap another non-cancelled show in the same hall/date |
| Seat templates | The hall must already have seat templates |

Response `201`: `ShowResponse`.

Errors: `400` validation, `404` movie/hall/seat template not found, `409` inactive hall, non-now-showing movie, or overlapping show.

#### `PUT /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Updates a show after validating movie status, hall status, and schedule conflicts. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `ShowResponse`.

Errors: `400` validation, `404` show/movie/hall not found, `409` schedule conflict or invalid movie/hall status.

#### `DELETE /api/admin/shows/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-deletes a show by setting status to `CANCELLED`. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

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
| `MovieResponse` | Legacy nested movie response currently used inside `ShowResponse`: `id`, `title`, `genre`, `durationMinutes`, `language`, `description`, `posterUrl`, `releaseDate`, `status`, `createdAt`, `updatedAt` |
| `PublicHallSummaryResponse` | `id`, `name`, `capacity` |
| `PublicHallDetailResponse` | `id`, `name`, `capacity`, `status` |
| `AdminHallSummaryResponse` | `id`, `name`, `capacity`, `layoutRef`, `status` |
| `AdminHallDetailResponse` | `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, `updatedAt` |
| `ShowResponse` | `id`, `movie`, `hall`, `status`, `showDate`, `showTime`, `endTime`, `createdAt`, `updatedAt` |
| `SeatResponse` | `id`, `rowLabel`, `seatNumber`, `seatCode`, `seatType`, `price`, `positionIndex`, `seatStatus` |
| `BookingResponse` | `bookingId`, `bookingStatus`, `showId`, `movieName`, `hallName`, `showDateTime`, `startTime`, `endTime`, `selectedSeats`, `totalPrice`, `bookingTime` |
| `UserResponse` | `id`, `name`, `email`, `role` |
| `AdminUserSummaryResponse` | `id`, `name`, `email`, `role`, `enabled`, `locked` |
| `AdminUserDetailResponse` | `id`, `name`, `email`, `role`, `authProvider`, `emailVerified`, `enabled`, `locked`, `failedLoginAttempts`, `lockedUntil`, `lastLoginAt`, `passwordChangedAt`, `createdAt`, `updatedAt` |
| `PageResponse<T>` | `content`, `page`, `size`, `totalElements`, `totalPages`, `last` |
