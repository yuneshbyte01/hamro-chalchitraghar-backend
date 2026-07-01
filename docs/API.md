# API Documentation

## Conventions

- Local base URL: `http://localhost:8080`
- JSON content type: `application/json`
- Auth header: `Authorization: Bearer <jwt>`

All non-empty success and error responses use `ApiResponse<T>`.

Success:

```json
{
  "success": true,
  "message": "Success message",
  "data": {},
  "errors": []
}
```

Error:

```json
{
  "success": false,
  "message": "Error message",
  "data": null,
  "errors": []
}
```

Validation errors use `message: "Validation failed"` and field-specific messages in `errors`.

Delete endpoints that currently return `204 No Content` do not include a response body.

## Endpoint Groups

- `/api/auth/**`: public authentication endpoints.
- `/api/public/**`: public browsing and health endpoints.
- `/api/customer/**`: authenticated customer endpoints. `CUSTOMER`, `STAFF`, and `ADMIN` roles can pass security for this group.
- `/api/staff/**`: staff endpoints. `STAFF` and `ADMIN` roles only.
- `/api/admin/**`: admin management endpoints. `ADMIN` role only.

## Auth

### POST `/api/auth/register`

Auth: public

Request:

```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

Response `201`:

```json
{
  "success": true,
  "message": "User registered successfully",
  "data": {
    "message": "User registered successfully",
    "email": "john@example.com"
  },
  "errors": []
}
```

Notes:

- Registration creates `CUSTOMER` users.
- Passwords are stored as BCrypt hashes.

### POST `/api/auth/login`

Auth: public

Request:

```json
{
  "email": "john@example.com",
  "password": "password123"
}
```

Response `200`:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "<jwt>",
    "email": "john@example.com",
    "name": "John Doe",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

Errors:

- `401`: invalid credentials.

### POST `/api/auth/refresh`

Auth: public

Request:

```json
{
  "token": "<current-jwt>"
}
```

Response `200`: same `data` shape as login with a new token.

Errors:

- `401`: invalid or expired token.

## Health

### GET `/api/public/health`

Auth: public

Response `200`:

```json
{
  "success": true,
  "message": "Health check successful",
  "data": {
    "status": "UP"
  },
  "errors": []
}
```

## Public Movies

Movie response inside `data`:

```json
{
  "id": 1,
  "title": "Movie Title",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "Nepali",
  "description": "Description",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2026-07-10",
  "status": "NOW_SHOWING",
  "createdAt": "2026-06-30T10:00:00",
  "updatedAt": "2026-06-30T10:00:00"
}
```

- `GET /api/public/movies`: all movies.
- `GET /api/public/movies/{id}`: movie by ID.
- `GET /api/public/movies/now-showing`: movies with status `NOW_SHOWING`.
- `GET /api/public/movies/upcoming`: movies with status `UPCOMING`.

## Public Halls

Hall response inside `data`:

```json
{
  "id": 1,
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "standard",
  "status": "ACTIVE",
  "createdAt": "2026-06-30T10:00:00",
  "updatedAt": "2026-06-30T10:00:00"
}
```

- `GET /api/public/halls`: all halls.
- `GET /api/public/halls/{id}`: hall by ID.
- `GET /api/public/halls/active`: active halls.

## Public Shows

Show response inside `data`:

```json
{
  "id": 1,
  "movie": { "...": "MovieResponse" },
  "hall": { "...": "HallResponse" },
  "status": "SCHEDULED",
  "showDate": "2026-07-20",
  "showTime": "14:00:00",
  "endTime": "16:30:00",
  "createdAt": "2026-06-30T10:00:00",
  "updatedAt": "2026-06-30T10:00:00"
}
```

- `GET /api/public/shows`: all shows.
- `GET /api/public/shows/{id}`: show by ID.
- `GET /api/public/shows/movie/{movieId}`: shows for a movie.
- `GET /api/public/shows?movieId={movieId}&date={yyyy-mm-dd}`: shows for a movie and date.
- `GET /api/public/shows/{showId}/seats`: seats for a show.

Seat response inside `data`:

```json
{
  "id": 1,
  "rowLabel": "A",
  "seatNumber": 1,
  "seatCode": "A1",
  "seatType": "PREMIUM",
  "price": 750.0,
  "positionIndex": 0,
  "seatStatus": "AVAILABLE"
}
```

## Customer Profile

### GET `/api/customer/profile`

Auth: authenticated user

Response `200`:

```json
{
  "success": true,
  "message": "Profile fetched successfully",
  "data": {
    "id": 1,
    "name": "John Doe",
    "email": "john@example.com",
    "role": "CUSTOMER"
  },
  "errors": []
}
```

## Customer Bookings

All customer booking endpoints are under `/api/customer/bookings`.

### POST `/api/customer/bookings/hold`

Holds seats for the current user for 10 minutes.

Request:

```json
{
  "showId": 1,
  "seatIds": [1, 2]
}
```

Response `200`:

```json
{
  "success": true,
  "message": "Seats held successfully",
  "data": {
    "message": "Seats held successfully",
    "showId": 1,
    "heldSeatCount": 2,
    "holdExpiresAt": "2026-07-01T12:10:00"
  },
  "errors": []
}
```

Rules:

- Seat IDs must not contain duplicates.
- Seats must belong to the show.
- Available seats become `LOCKED`.
- A user can refresh their own active lock.
- Another user cannot hold or book seats locked by someone else.
- Expired locks are cleared before reuse.

### POST `/api/customer/bookings`

Creates an `INITIATED` booking from available seats or seats held by the current user.

Request:

```json
{
  "showId": 1,
  "seatIds": [1, 2]
}
```

Response `201`:

```json
{
  "success": true,
  "message": "Booking created successfully",
  "data": {
    "bookingId": 10,
    "bookingStatus": "INITIATED",
    "showId": 1,
    "movieName": "Movie Title",
    "hallName": "Hall A",
    "showDateTime": "2026-07-20T14:00:00",
    "startTime": "14:00",
    "endTime": "16:30",
    "selectedSeats": [{ "...": "SeatResponse" }],
    "totalPrice": 1500.0,
    "bookingTime": "2026-07-01T12:00:00"
  },
  "errors": []
}
```

### POST `/api/customer/bookings/{bookingId}/confirm`

Confirms an `INITIATED` booking owned by the current user.

Response `200`: booking response with `bookingStatus` set to `CONFIRMED`; selected seats become `BOOKED`.

### GET `/api/customer/bookings/my`

Response `200`: current user's bookings ordered by booking time descending.

### GET `/api/customer/bookings/{bookingId}`

Response `200`: booking response.

Access rules:

- Customers can view their own bookings.
- Service logic allows `STAFF` and `ADMIN` to view any booking.

### POST `/api/customer/bookings/{bookingId}/cancel`

Cancels an `INITIATED` booking owned by the current user.

Response `200`: booking response with `bookingStatus` set to `CANCELLED`; selected seats become `AVAILABLE`.

Rules:

- Only `INITIATED` bookings can be cancelled.
- The show time must still be in the future.

## Staff Bookings

Implemented staff endpoint:

- `GET /api/staff/bookings/{bookingId}`: returns a booking by ID for `STAFF` and `ADMIN`.

Planned but not implemented:

- Staff check-in endpoint.
- Staff booking list endpoint.
- Staff show/seat operational endpoints.

## Admin Movies

Auth: `ADMIN`

Movie request:

```json
{
  "title": "Movie Title",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "Nepali",
  "description": "Description",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2026-07-10",
  "status": "NOW_SHOWING"
}
```

- `GET /api/admin/movies`: all movies.
- `GET /api/admin/movies/{id}`: movie by ID.
- `POST /api/admin/movies`: `201`, creates a movie.
- `PUT /api/admin/movies/{id}`: updates a movie.
- `DELETE /api/admin/movies/{id}`: `204`, soft-deletes by setting status to `ENDED`.

## Admin Halls

Auth: `ADMIN`

Hall request:

```json
{
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "standard",
  "status": "ACTIVE"
}
```

- `GET /api/admin/halls`: all halls.
- `GET /api/admin/halls/{id}`: hall by ID.
- `GET /api/admin/halls/active`: active halls.
- `POST /api/admin/halls`: `201`, creates a hall.
- `PUT /api/admin/halls/{id}`: updates a hall.
- `DELETE /api/admin/halls/{id}`: `204`, soft-deletes by setting status to `INACTIVE`.
- `POST /api/admin/halls/{hallId}/seat-layout`: creates 188 seat templates for the hall.

## Admin Shows

Auth: `ADMIN`

Show request:

```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-07-20",
  "showTime": "14:00:00",
  "endTime": "16:30:00"
}
```

- `GET /api/admin/shows`: all shows.
- `GET /api/admin/shows/{id}`: show by ID.
- `POST /api/admin/shows`: `201`, creates a show and generates seats from the hall's seat templates.
- `PUT /api/admin/shows/{id}`: updates a show.
- `DELETE /api/admin/shows/{id}`: `204`, soft-deletes by setting status to `CANCELLED`.

Rules:

- Movie must be `NOW_SHOWING`.
- Hall must not be `INACTIVE`.
- Hall must already have seat templates.
- Show time must not overlap another non-cancelled show in the same hall on the same date.

## Admin Users

Auth: `ADMIN`

- `GET /api/admin/users`: all users.
- `GET /api/admin/users/{id}`: user by ID.

Admin staff creation endpoint: planned, not implemented.

## Common Error Status Codes

- `400`: validation error or bad request.
- `401`: missing, invalid, expired token, or invalid credentials.
- `403`: authenticated but not allowed.
- `404`: resource not found.
- `409`: conflict such as locked/booked seats or show overlap.
- `500`: unexpected server error with a generic message.
