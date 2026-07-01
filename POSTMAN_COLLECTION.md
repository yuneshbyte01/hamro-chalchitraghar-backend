# Hamro Chalchitraghar V2 API Quick Reference

Base URL:

```text
http://localhost:8080
```

Auth header for protected endpoints:

```text
Authorization: Bearer <jwt>
```

## Standard Response

```json
{
  "success": true,
  "message": "Success message",
  "data": {},
  "errors": []
}
```

Errors use the same fields with `success: false` and `data: null`.

## Auth

### Register

`POST /api/auth/register`

```json
{
  "name": "Customer One",
  "email": "customer@example.com",
  "password": "password123"
}
```

### Login

`POST /api/auth/login`

```json
{
  "email": "customer@example.com",
  "password": "password123"
}
```

Copy `data.token` from the response for protected requests.

### Refresh

`POST /api/auth/refresh`

```json
{
  "token": "<current-jwt>"
}
```

## Public

- `GET /api/public/health`
- `GET /api/public/movies`
- `GET /api/public/movies/{id}`
- `GET /api/public/movies/now-showing`
- `GET /api/public/movies/upcoming`
- `GET /api/public/halls`
- `GET /api/public/halls/{id}`
- `GET /api/public/halls/active`
- `GET /api/public/shows`
- `GET /api/public/shows/{id}`
- `GET /api/public/shows/movie/{movieId}`
- `GET /api/public/shows?movieId={movieId}&date={yyyy-mm-dd}`
- `GET /api/public/shows/{showId}/seats`

## Customer

### Profile

`GET /api/customer/profile`

### Hold Seats

`POST /api/customer/bookings/hold`

```json
{
  "showId": 1,
  "seatIds": [1, 2]
}
```

### Create Booking

`POST /api/customer/bookings`

```json
{
  "showId": 1,
  "seatIds": [1, 2]
}
```

### Confirm Booking

`POST /api/customer/bookings/{bookingId}/confirm`

### My Bookings

`GET /api/customer/bookings/my`

### Booking Detail

`GET /api/customer/bookings/{bookingId}`

### Cancel Booking

`POST /api/customer/bookings/{bookingId}/cancel`

## Staff

Implemented:

- `GET /api/staff/bookings/{bookingId}`

Planned:

- Staff check-in endpoint.
- Staff booking list endpoint.

## Admin

### Movie Request

```json
{
  "title": "Movie Title",
  "genre": "Drama",
  "durationMinutes": 120,
  "language": "Nepali",
  "description": "Movie description",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2026-07-10",
  "status": "NOW_SHOWING"
}
```

Movie endpoints:

- `GET /api/admin/movies`
- `GET /api/admin/movies/{id}`
- `POST /api/admin/movies`
- `PUT /api/admin/movies/{id}`
- `DELETE /api/admin/movies/{id}`

### Hall Request

```json
{
  "name": "Hall A",
  "capacity": 188,
  "layoutRef": "standard",
  "status": "ACTIVE"
}
```

Hall endpoints:

- `GET /api/admin/halls`
- `GET /api/admin/halls/{id}`
- `GET /api/admin/halls/active`
- `POST /api/admin/halls`
- `PUT /api/admin/halls/{id}`
- `DELETE /api/admin/halls/{id}`
- `POST /api/admin/halls/{hallId}/seat-layout`

### Show Request

```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-07-20",
  "showTime": "14:00:00",
  "endTime": "16:30:00"
}
```

Show endpoints:

- `GET /api/admin/shows`
- `GET /api/admin/shows/{id}`
- `POST /api/admin/shows`
- `PUT /api/admin/shows/{id}`
- `DELETE /api/admin/shows/{id}`

User endpoints:

- `GET /api/admin/users`
- `GET /api/admin/users/{id}`

## Recommended Manual Flow

1. Register or create a user.
2. Login and copy `data.token`.
3. Use an admin token to create a hall.
4. Generate seat layout with `POST /api/admin/halls/{hallId}/seat-layout`.
5. Create a `NOW_SHOWING` movie.
6. Create a show for the movie and hall.
7. Fetch seats with `GET /api/public/shows/{showId}/seats`.
8. Use a customer token to hold seats.
9. Create a booking.
10. Confirm or cancel the booking.
