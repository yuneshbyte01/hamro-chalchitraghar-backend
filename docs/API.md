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
| Description | Lists all movies. |
| Request body | None |
| Validation | None |

Response `200`:

```json
{
  "success": true,
  "message": "Movies fetched successfully",
  "data": [
    {
      "id": 1,
      "title": "Jatra",
      "genre": "Comedy",
      "durationMinutes": 125,
      "language": "Nepali",
      "description": "A Nepali comedy movie.",
      "posterUrl": "https://example.com/posters/jatra.jpg",
      "releaseDate": "2026-08-15",
      "status": "NOW_SHOWING",
      "createdAt": "2026-07-01T10:00:00",
      "updatedAt": "2026-07-01T10:00:00"
    }
  ],
  "errors": []
}
```

### `GET /api/public/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one movie by ID. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `MovieResponse` in the standard wrapper.

Errors: `400` invalid ID type, `404` movie not found.

### `GET /api/public/movies/now-showing`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists movies where `status` is `NOW_SHOWING`, ordered by release date ascending. |
| Request body | None |
| Validation | None |

Response `200`: list of `MovieResponse`.

### `GET /api/public/movies/upcoming`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists movies where `status` is `UPCOMING`, ordered by release date ascending. |
| Request body | None |
| Validation | None |

Response `200`: list of `MovieResponse`.

### `GET /api/public/halls`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists all halls. |
| Request body | None |
| Validation | None |

Response `200`:

```json
{
  "success": true,
  "message": "Halls fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "Hall A",
      "capacity": 188,
      "layoutRef": "standard",
      "status": "ACTIVE",
      "createdAt": "2026-07-01T10:00:00",
      "updatedAt": "2026-07-01T10:00:00"
    }
  ],
  "errors": []
}
```

### `GET /api/public/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Fetches one hall by ID. |
| Request body | None |
| Validation | `id` must be numeric |

Response `200`: `HallResponse`.

Errors: `400` invalid ID type, `404` hall not found.

### `GET /api/public/halls/active`

| Field | Value |
| --- | --- |
| Authentication | Public |
| Description | Lists halls where `status` is `ACTIVE`. |
| Request body | None |
| Validation | None |

Response `200`: list of `HallResponse`.

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
| Description | Lists all movies for admin. |
| Request body | None |
| Validation | ADMIN token |

Response `200`: list of `MovieResponse`.

#### `GET /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one movie by ID for admin. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `MovieResponse`.

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

Response `201`: `MovieResponse`.

#### `PUT /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Replaces movie fields from `MovieRequest`. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `MovieResponse`.

Errors: `400` validation, `404` movie not found.

#### `DELETE /api/admin/movies/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Soft-deletes a movie by setting status to `ENDED`. |
| Request body | None |
| Validation | `id` numeric |

Response `204`: empty body.

Errors: `404` movie not found.

### Halls

#### `GET /api/admin/halls`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists all halls for admin. |
| Request body | None |
| Validation | ADMIN token |

Response `200`: list of `HallResponse`.

#### `GET /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one hall by ID for admin. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `HallResponse`.

Errors: `404` hall not found.

#### `GET /api/admin/halls/active`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Lists active halls. |
| Request body | None |
| Validation | ADMIN token |

Response `200`: list of `HallResponse`.

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

Response `201`: `HallResponse`.

Errors: `400` duplicate hall name or validation failure.

#### `PUT /api/admin/halls/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Replaces hall fields from `HallRequest`. |
| Validation | Same body rules as create plus numeric `id` |

Response `200`: `HallResponse`.

Errors: `404` hall not found.

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
| Description | Lists all users without password hashes. |
| Request body | None |
| Validation | ADMIN token |

Response `200`:

```json
{
  "success": true,
  "message": "Users fetched successfully",
  "data": [
    {
      "id": 1,
      "name": "Aarav Sharma",
      "email": "aarav@example.com",
      "role": "CUSTOMER"
    }
  ],
  "errors": []
}
```

#### `GET /api/admin/users/{id}`

| Field | Value |
| --- | --- |
| Authentication | ADMIN |
| Description | Fetches one user without password hash. |
| Request body | None |
| Validation | `id` numeric |

Response `200`: `UserResponse`.

Errors: `404` user not found.

## Response DTO Summary

| DTO | Fields |
| --- | --- |
| `MovieResponse` | `id`, `title`, `genre`, `durationMinutes`, `language`, `description`, `posterUrl`, `releaseDate`, `status`, `createdAt`, `updatedAt` |
| `HallResponse` | `id`, `name`, `capacity`, `layoutRef`, `status`, `createdAt`, `updatedAt` |
| `ShowResponse` | `id`, `movie`, `hall`, `status`, `showDate`, `showTime`, `endTime`, `createdAt`, `updatedAt` |
| `SeatResponse` | `id`, `rowLabel`, `seatNumber`, `seatCode`, `seatType`, `price`, `positionIndex`, `seatStatus` |
| `BookingResponse` | `bookingId`, `bookingStatus`, `showId`, `movieName`, `hallName`, `showDateTime`, `startTime`, `endTime`, `selectedSeats`, `totalPrice`, `bookingTime` |
| `UserResponse` | `id`, `name`, `email`, `role` |
