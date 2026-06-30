# API Documentation

## Conventions

- Local base URL: `http://localhost:8080`
- Auth header: `Authorization: Bearer <jwt>`
- Standard error body:

```json
{
  "timestamp": "2026-06-30T10:15:30.000+05:45",
  "status": 400,
  "error": "Bad Request",
  "message": "field: validation message",
  "path": "/api/example"
}
```

JWT filter token failures may return only:

```json
{ "error": "Invalid token" }
```

or:

```json
{ "error": "Token expired" }
```

## Auth

### POST `/api/auth/register`

Auth: Public

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
  "message": "User registered successfully",
  "email": "john@example.com"
}
```

Errors: `400` validation or duplicate email, `409` data integrity conflict.

### POST `/api/auth/login`

Auth: Public

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
  "token": "<jwt>",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "CUSTOMER"
}
```

Errors: `401` invalid credentials, `500` if email is not found because `UserService.getUserByEmail` throws a plain `RuntimeException`.

### POST `/api/auth/refresh`

Auth: Public

Request:

```json
{ "token": "<current-jwt>" }
```

Response `200`: same shape as login.

Errors: `401` invalid or expired token.

## Health

### GET `/api/health`

Auth: Public

Response `200`:

```json
{ "status": "UP" }
```

## Movies

Movie response:

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

### GET `/api/movies`

Auth: Public

Response `200`: array of movie responses.

### GET `/api/movies/{id}`

Auth: Public

Response `200`: movie response.

Errors: `404` when not found.

### GET `/api/movies/now-showing`

Auth: Public

Response `200`: movies with status `NOW_SHOWING`, ordered by release date ascending.

### GET `/api/movies/upcoming`

Auth: Public

Response `200`: movies with status `UPCOMING`, ordered by release date ascending.

## Halls

Hall response:

```json
{
  "id": 1,
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "default",
  "status": "ACTIVE",
  "createdAt": "2026-06-30T10:00:00",
  "updatedAt": "2026-06-30T10:00:00"
}
```

### GET `/api/halls`

Auth: Public

Response `200`: array of hall responses.

### GET `/api/halls/{id}`

Auth: Public

Response `200`: hall response.

Errors: `404` when not found.

### GET `/api/halls/active`

Auth: Public

Response `200`: halls with status `ACTIVE`.

## Shows

Show response:

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

### GET `/api/shows`

Auth: Public

Response `200`: array of show responses.

### GET `/api/shows/{id}`

Auth: Public

Response `200`: show response.

Errors: `404` when not found, cancelled, completed, or when the associated movie is not `NOW_SHOWING`.

### GET `/api/shows/movie/{movieId}`

Auth: Public

Response `200`: array of show responses for the movie.

### GET `/api/shows?movieId={movieId}&date={yyyy-mm-dd}`

Auth: Public

Response `200`: non-cancelled and non-completed shows for a `NOW_SHOWING` movie on the date. Returns an empty array if the movie is not `NOW_SHOWING`.

### GET `/api/shows/{showId}/seats`

Auth: Public

Response `200`:

```json
[
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
]
```

## Bookings

All booking endpoints require authentication. Spring Security accepts any authenticated role for `/api/bookings/**`; service logic enforces owner checks on detail, confirm, and cancel.

### POST `/api/bookings/validate`

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
  "message": "Seats validated and locked successfully",
  "showId": 1,
  "lockedSeatCount": 2
}
```

Errors: `400` invalid seat selection, `409` locked/booked seats, `401` unauthenticated.

### POST `/api/bookings`

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
  "bookingTime": "2026-06-30T10:00:00"
}
```

Errors: `400`, `404`, `409`, `401`.

### POST `/api/bookings/{bookingId}/confirm`

Response `200`: booking response with `bookingStatus` set to `CONFIRMED` and seats set to `BOOKED`.

Errors: `400` invalid booking state, `403` not owner, `404`, `409`.

### GET `/api/bookings/my`

Response `200`: current user's bookings ordered by booking time descending.

### GET `/api/bookings/{bookingId}`

Response `200`: booking response.

Access: booking owner, `ADMIN`, or `STAFF` in service logic.

Errors: `403` when not allowed, `404` when not found.

### POST `/api/bookings/{bookingId}/cancel`

Response `200`: booking response with `bookingStatus` set to `CANCELLED` and seats set to `AVAILABLE`.

Rules: only `INITIATED` bookings can be cancelled; show time must be in the future.

Errors: `400` invalid state, `403` not owner, `404`.

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

- `GET /api/admin/movies`: array of movie responses.
- `GET /api/admin/movies/{id}`: movie response.
- `POST /api/admin/movies`: `201` movie response.
- `PUT /api/admin/movies/{id}`: updated movie response.
- `DELETE /api/admin/movies/{id}`: `204`; sets status to `ENDED`.

## Admin Halls

Auth: `ADMIN`

Hall request:

```json
{
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "default",
  "status": "ACTIVE"
}
```

- `GET /api/admin/halls`: array of hall responses.
- `GET /api/admin/halls/{id}`: hall response.
- `GET /api/admin/halls/active`: active hall responses.
- `POST /api/admin/halls`: `201` hall response.
- `PUT /api/admin/halls/{id}`: updated hall response.
- `DELETE /api/admin/halls/{id}`: `204`; sets status to `INACTIVE`.
- `POST /api/admin/halls/{hallId}/seat-layout`: `200` empty body; creates seat templates.

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

- `GET /api/admin/shows`: array of show responses.
- `GET /api/admin/shows/{id}`: show response.
- `POST /api/admin/shows`: `201` show response; also generates seats for the show.
- `PUT /api/admin/shows/{id}`: updated show response.
- `DELETE /api/admin/shows/{id}`: `204`; sets status to `CANCELLED`.

Rules: movie must be `NOW_SHOWING`, hall must not be `INACTIVE`, and show time must not overlap another non-cancelled show in the same hall on the same date.

## Admin Users

Auth: `ADMIN`

User response:

```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

- `GET /api/admin/users`: array of user responses.
- `GET /api/admin/users/{id}`: user response.
