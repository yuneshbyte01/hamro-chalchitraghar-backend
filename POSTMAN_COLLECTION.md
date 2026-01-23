# Hamro Chalchitraghar API - Postman Collection Documentation

## Base URL
```
http://localhost:8080
```

## Authentication
The API uses JWT (JSON Web Token) authentication. After login, include the token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. Authentication Endpoints

### 1.1 Register User
**POST** `/api/auth/register`

**Description:** Register a new user account.

**Authentication:** Not required (Public)

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Request Headers:**
```
Content-Type: application/json
```

**Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "email": "john.doe@example.com"
}
```

**Validation Rules:**
- `name`: Required, cannot be blank
- `email`: Required, must be valid email format
- `password`: Required, minimum 8 characters

---

### 1.2 Login User
**POST** `/api/auth/login`

**Description:** Authenticate user and receive JWT token.

**Authentication:** Not required (Public)

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Request Headers:**
```
Content-Type: application/json
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "john.doe@example.com",
  "name": "John Doe",
  "role": "CUSTOMER"
}
```

**Validation Rules:**
- `email`: Required, must be valid email format
- `password`: Required, minimum 8 characters

**Note:** Save the `token` from the response to use in subsequent authenticated requests.

---

## 2. Movie Endpoints (Public)

### 2.1 Get All Movies
**GET** `/api/movies`

**Description:** Retrieve all movies.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Movie Title",
    "genre": "Action",
    "durationMinutes": 120,
    "language": "English",
    "description": "Movie description",
    "posterUrl": "https://example.com/poster.jpg",
    "releaseDate": "2024-01-15",
    "status": "NOW_SHOWING"
  }
]
```

---

### 2.2 Get Movie by ID
**GET** `/api/movies/{id}`

**Description:** Retrieve a specific movie by ID.

**Authentication:** Not required (Public)

**Path Parameters:**
- `id` (Long): Movie ID

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Movie Title",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "English",
  "description": "Movie description",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2024-01-15",
  "status": "NOW_SHOWING"
}
```

---

### 2.3 Get Now Showing Movies
**GET** `/api/movies/now-showing`

**Description:** Retrieve all movies currently showing.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Movie Title",
    "genre": "Action",
    "durationMinutes": 120,
    "language": "English",
    "description": "Movie description",
    "posterUrl": "https://example.com/poster.jpg",
    "releaseDate": "2024-01-15",
    "status": "NOW_SHOWING"
  }
]
```

---

### 2.4 Get Upcoming Movies
**GET** `/api/movies/upcoming`

**Description:** Retrieve all upcoming movies.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 2,
    "title": "Upcoming Movie",
    "genre": "Drama",
    "durationMinutes": 110,
    "language": "English",
    "description": "Upcoming movie description",
    "posterUrl": "https://example.com/poster2.jpg",
    "releaseDate": "2024-02-20",
    "status": "UPCOMING"
  }
]
```

---

## 3. Hall Endpoints (Public)

### 3.1 Get All Halls
**GET** `/api/halls`

**Description:** Retrieve all halls.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Hall A",
    "capacity": 100,
    "layoutRef": "layout-001",
    "status": "ACTIVE"
  }
]
```

---

### 3.2 Get Hall by ID
**GET** `/api/halls/{id}`

**Description:** Retrieve a specific hall by ID.

**Authentication:** Not required (Public)

**Path Parameters:**
- `id` (Long): Hall ID

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Hall A",
  "capacity": 100,
  "layoutRef": "layout-001",
  "status": "ACTIVE"
}
```

---

### 3.3 Get Active Halls
**GET** `/api/halls/active`

**Description:** Retrieve all active halls.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Hall A",
    "capacity": 100,
    "layoutRef": "layout-001",
    "status": "ACTIVE"
  }
]
```

---

## 4. Show Endpoints (Public)

### 4.1 Get All Shows
**GET** `/api/shows`

**Description:** Retrieve all shows.

**Authentication:** Not required (Public)

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "movieId": 1,
    "movieName": "Movie Title",
    "hallId": 1,
    "hallName": "Hall A",
    "price": 500.0,
    "showDate": "2024-01-20",
    "showTime": "14:00:00",
    "endTime": "16:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### 4.2 Get Show by ID
**GET** `/api/shows/{id}`

**Description:** Retrieve a specific show by ID.

**Authentication:** Not required (Public)

**Path Parameters:**
- `id` (Long): Show ID

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
{
  "id": 1,
  "movieId": 1,
  "movieName": "Movie Title",
  "hallId": 1,
  "hallName": "Hall A",
  "price": 500.0,
  "showDate": "2024-01-20",
  "showTime": "14:00:00",
  "endTime": "16:00:00",
  "status": "SCHEDULED"
}
```

---

### 4.3 Get Shows by Movie
**GET** `/api/shows/movie/{movieId}`

**Description:** Retrieve all shows for a specific movie.

**Authentication:** Not required (Public)

**Path Parameters:**
- `movieId` (Long): Movie ID

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "movieId": 1,
    "movieName": "Movie Title",
    "hallId": 1,
    "hallName": "Hall A",
    "price": 500.0,
    "showDate": "2024-01-20",
    "showTime": "14:00:00",
    "endTime": "16:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### 4.4 Get Shows by Movie and Date
**GET** `/api/shows?movieId={movieId}&date={date}`

**Description:** Retrieve shows for a specific movie on a specific date.

**Authentication:** Not required (Public)

**Query Parameters:**
- `movieId` (Long): Movie ID
- `date` (LocalDate): Show date in format `YYYY-MM-DD`

**Example:**
```
GET /api/shows?movieId=1&date=2024-01-20
```

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "movieId": 1,
    "movieName": "Movie Title",
    "hallId": 1,
    "hallName": "Hall A",
    "price": 500.0,
    "showDate": "2024-01-20",
    "showTime": "14:00:00",
    "endTime": "16:00:00",
    "status": "SCHEDULED"
  }
]
```

---

## 5. Seat Endpoints (Public)

### 5.1 Get Seats for Show
**GET** `/api/shows/{showId}/seats`

**Description:** Retrieve all seats for a specific show, ordered by position index.

**Authentication:** Not required (Public)

**Path Parameters:**
- `showId` (Long): Show ID

**Request Headers:**
```
None required
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "seatCode": "A1",
    "rowLabel": "A",
    "seatNumber": 1,
    "seatType": "REGULAR",
    "status": "AVAILABLE",
    "positionIndex": 0
  },
  {
    "id": 2,
    "seatCode": "A2",
    "rowLabel": "A",
    "seatNumber": 2,
    "seatType": "REGULAR",
    "status": "BOOKED",
    "positionIndex": 1
  }
]
```

**Seat Status Values:**
- `AVAILABLE`: Seat is available for booking
- `LOCKED`: Seat is temporarily locked (10 minutes)
- `BOOKED`: Seat is already booked

---

## 6. Booking Endpoints (Authenticated)

### 6.1 Validate and Lock Seats
**POST** `/api/bookings/validate`

**Description:** Validate seat availability and lock seats for 10 minutes before booking.

**Authentication:** Required (JWT Token)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "showId": 1,
  "seatIds": [1, 2, 3]
}
```

**Validation Rules:**
- `showId`: Required, must be a valid show ID
- `seatIds`: Required, must contain at least one seat ID, no duplicates allowed

**Response (200 OK):**
```json
{
  "message": "Seats validated and locked successfully",
  "showId": 1,
  "lockedSeatCount": 3
}
```

**Error Responses:**
- `400 Bad Request`: Invalid request (missing fields, duplicate seats)
- `401 Unauthorized`: Missing or invalid JWT token
- `404 Not Found`: Show or seat not found
- `409 Conflict`: Seat already booked or locked by another user

---

### 6.2 Create Booking
**POST** `/api/bookings`

**Description:** Create a new booking for the authenticated user.

**Authentication:** Required (JWT Token - CUSTOMER role)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "showId": 1,
  "seatIds": [1, 2, 3]
}
```

**Validation Rules:**
- `showId`: Required, must be a valid show ID
- `seatIds`: Required, must contain at least one seat ID, no duplicates allowed

**Response (201 Created):**
```json
{
  "bookingId": 1,
  "bookingStatus": "CONFIRMED",
  "showId": 1,
  "movieName": "Movie Title",
  "showDateTime": "2024-01-20T14:00:00",
  "selectedSeats": [
    {
      "id": 1,
      "seatCode": "A1",
      "rowLabel": "A",
      "seatNumber": 1,
      "seatType": "REGULAR",
      "status": "BOOKED",
      "positionIndex": 0
    }
  ],
  "totalPrice": 1500.0,
  "bookingTime": "2024-01-19T10:30:00"
}
```

**Booking Status Values:**
- `CONFIRMED`: Booking is confirmed
- `CANCELLED`: Booking is cancelled

**Error Responses:**
- `400 Bad Request`: Invalid request (missing fields, duplicate seats)
- `401 Unauthorized`: Missing or invalid JWT token
- `403 Forbidden`: User does not have CUSTOMER role
- `404 Not Found`: Show or seat not found
- `409 Conflict`: Seat already booked or not locked by current user

**Note:** Seats must be locked first using the `/api/bookings/validate` endpoint before creating a booking.

---

## 7. Admin Endpoints (Admin Only)

All admin endpoints require **ADMIN** role authentication.

### 7.1 Admin Test Endpoint
**GET** `/api/admin`

**Description:** Test endpoint to verify admin access.

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```
Hello, world!
```

---

## 8. Admin Movie Management

### 8.1 Get All Movies (Admin)
**GET** `/api/admin/movies`

**Description:** Retrieve all movies (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "title": "Movie Title",
    "genre": "Action",
    "durationMinutes": 120,
    "language": "English",
    "description": "Movie description",
    "posterUrl": "https://example.com/poster.jpg",
    "releaseDate": "2024-01-15",
    "status": "NOW_SHOWING"
  }
]
```

---

### 8.2 Get Movie by ID (Admin)
**GET** `/api/admin/movies/{id}`

**Description:** Retrieve a specific movie by ID (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Movie ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Movie Title",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "English",
  "description": "Movie description",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2024-01-15",
  "status": "NOW_SHOWING"
}
```

---

### 8.3 Create Movie
**POST** `/api/admin/movies`

**Description:** Create a new movie.

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "title": "New Movie",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "English",
  "description": "Movie description here",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2024-02-15",
  "status": "UPCOMING"
}
```

**Validation Rules:**
- `title`: Required, cannot be blank
- `genre`: Required, cannot be blank
- `durationMinutes`: Required, must be positive or zero
- `language`: Required, cannot be blank
- `description`: Required, cannot be blank
- `posterUrl`: Required, cannot be blank
- `releaseDate`: Required, must be a valid date
- `status`: Required, must be one of: `NOW_SHOWING`, `UPCOMING`, `ENDED`

**Response (201 Created):**
```json
{
  "id": 1,
  "title": "New Movie",
  "genre": "Action",
  "durationMinutes": 120,
  "language": "English",
  "description": "Movie description here",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2024-02-15",
  "status": "UPCOMING"
}
```

---

### 8.4 Update Movie
**PUT** `/api/admin/movies/{id}`

**Description:** Update an existing movie.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Movie ID

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "title": "Updated Movie Title",
  "genre": "Drama",
  "durationMinutes": 110,
  "language": "English",
  "description": "Updated description",
  "posterUrl": "https://example.com/new-poster.jpg",
  "releaseDate": "2024-02-20",
  "status": "NOW_SHOWING"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "title": "Updated Movie Title",
  "genre": "Drama",
  "durationMinutes": 110,
  "language": "English",
  "description": "Updated description",
  "posterUrl": "https://example.com/new-poster.jpg",
  "releaseDate": "2024-02-20",
  "status": "NOW_SHOWING"
}
```

---

### 8.5 Delete Movie
**DELETE** `/api/admin/movies/{id}`

**Description:** Soft delete a movie by setting its status to ENDED.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Movie ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (204 No Content):**
```
(No response body)
```

---

## 9. Admin Hall Management

### 9.1 Get All Halls (Admin)
**GET** `/api/admin/halls`

**Description:** Retrieve all halls (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Hall A",
    "capacity": 100,
    "layoutRef": "layout-001",
    "status": "ACTIVE"
  }
]
```

---

### 9.2 Get Hall by ID (Admin)
**GET** `/api/admin/halls/{id}`

**Description:** Retrieve a specific hall by ID (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Hall ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Hall A",
  "capacity": 100,
  "layoutRef": "layout-001",
  "status": "ACTIVE"
}
```

---

### 9.3 Create Hall
**POST** `/api/admin/halls`

**Description:** Create a new hall.

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "name": "Hall B",
  "capacity": 150,
  "layoutRef": "layout-002",
  "status": "ACTIVE"
}
```

**Validation Rules:**
- `name`: Required, cannot be blank
- `capacity`: Required, must be positive or zero (at least 1)
- `layoutRef`: Required, cannot be blank
- `status`: Required, must be one of: `ACTIVE`, `INACTIVE`

**Response (201 Created):**
```json
{
  "id": 2,
  "name": "Hall B",
  "capacity": 150,
  "layoutRef": "layout-002",
  "status": "ACTIVE"
}
```

---

### 9.4 Update Hall
**PUT** `/api/admin/halls/{id}`

**Description:** Update an existing hall.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Hall ID

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "name": "Updated Hall Name",
  "capacity": 200,
  "layoutRef": "layout-002-updated",
  "status": "ACTIVE"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "name": "Updated Hall Name",
  "capacity": 200,
  "layoutRef": "layout-002-updated",
  "status": "ACTIVE"
}
```

---

### 9.5 Delete Hall
**DELETE** `/api/admin/halls/{id}`

**Description:** Soft delete a hall by setting its status to INACTIVE.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Hall ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (204 No Content):**
```
(No response body)
```

---

### 9.6 Get Active Halls (Admin)
**GET** `/api/admin/halls/active`

**Description:** Retrieve all active halls (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "name": "Hall A",
    "capacity": 100,
    "layoutRef": "layout-001",
    "status": "ACTIVE"
  }
]
```

---

### 9.7 Generate Seat Layout
**POST** `/api/admin/halls/{hallId}/seat-layout`

**Description:** Generate seat layout templates for a hall.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `hallId` (Long): Hall ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```
(No response body)
```

---

## 10. Admin Show Management

### 10.1 Get All Shows (Admin)
**GET** `/api/admin/shows`

**Description:** Retrieve all shows (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "movieId": 1,
    "movieName": "Movie Title",
    "hallId": 1,
    "hallName": "Hall A",
    "price": 500.0,
    "showDate": "2024-01-20",
    "showTime": "14:00:00",
    "endTime": "16:00:00",
    "status": "SCHEDULED"
  }
]
```

---

### 10.2 Get Show by ID (Admin)
**GET** `/api/admin/shows/{id}`

**Description:** Retrieve a specific show by ID (admin view).

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Show ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (200 OK):**
```json
{
  "id": 1,
  "movieId": 1,
  "movieName": "Movie Title",
  "hallId": 1,
  "hallName": "Hall A",
  "price": 500.0,
  "showDate": "2024-01-20",
  "showTime": "14:00:00",
  "endTime": "16:00:00",
  "status": "SCHEDULED"
}
```

---

### 10.3 Create Show
**POST** `/api/admin/shows`

**Description:** Create a new show.

**Authentication:** Required (JWT Token - ADMIN role)

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "movieId": 1,
  "hallId": 1,
  "price": 500.0,
  "showDate": "2024-02-20",
  "showTime": "14:00:00",
  "endTime": "16:00:00"
}
```

**Validation Rules:**
- `movieId`: Required, must be a valid movie ID
- `hallId`: Required, must be a valid hall ID
- `price`: Required, must be positive (greater than 0)
- `showDate`: Required, must be a future date
- `showTime`: Required, must be a valid time
- `endTime`: Required, must be a valid time

**Date/Time Format:**
- `showDate`: `YYYY-MM-DD` (e.g., "2024-02-20")
- `showTime`: `HH:mm:ss` (e.g., "14:00:00")
- `endTime`: `HH:mm:ss` (e.g., "16:00:00")

**Response (201 Created):**
```json
{
  "id": 1,
  "movieId": 1,
  "movieName": "Movie Title",
  "hallId": 1,
  "hallName": "Hall A",
  "price": 500.0,
  "showDate": "2024-02-20",
  "showTime": "14:00:00",
  "endTime": "16:00:00",
  "status": "SCHEDULED"
}
```

**Error Responses:**
- `409 Conflict`: Show time conflicts with existing show in the same hall

---

### 10.4 Update Show
**PUT** `/api/admin/shows/{id}`

**Description:** Update an existing show.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Show ID

**Request Headers:**
```
Content-Type: application/json
Authorization: Bearer <your_jwt_token>
```

**Request Body:**
```json
{
  "movieId": 1,
  "hallId": 1,
  "price": 600.0,
  "showDate": "2024-02-21",
  "showTime": "15:00:00",
  "endTime": "17:00:00"
}
```

**Response (200 OK):**
```json
{
  "id": 1,
  "movieId": 1,
  "movieName": "Movie Title",
  "hallId": 1,
  "hallName": "Hall A",
  "price": 600.0,
  "showDate": "2024-02-21",
  "showTime": "15:00:00",
  "endTime": "17:00:00",
  "status": "SCHEDULED"
}
```

---

### 10.5 Delete Show
**DELETE** `/api/admin/shows/{id}`

**Description:** Soft delete a show by setting its status to CANCELLED.

**Authentication:** Required (JWT Token - ADMIN role)

**Path Parameters:**
- `id` (Long): Show ID

**Request Headers:**
```
Authorization: Bearer <your_jwt_token>
```

**Response (204 No Content):**
```
(No response body)
```

---

## 11. Common Error Responses

### 400 Bad Request
```json
{
  "message": "Validation error",
  "errors": [
    "Email is required",
    "Password must be at least 8 characters"
  ]
}
```

### 401 Unauthorized
```json
{
  "message": "Unauthorized",
  "error": "Invalid or missing JWT token"
}
```

### 403 Forbidden
```json
{
  "message": "Forbidden",
  "error": "Access denied. Required role: ADMIN"
}
```

### 404 Not Found
```json
{
  "message": "Resource not found",
  "error": "Movie with ID 999 not found"
}
```

### 409 Conflict
```json
{
  "message": "Conflict",
  "error": "Seat is already booked"
}
```

### 500 Internal Server Error
```json
{
  "message": "Internal server error",
  "error": "An unexpected error occurred"
}
```

---

## 12. Postman Collection Setup

### Environment Variables
Create a Postman environment with the following variables:

| Variable | Initial Value | Current Value |
|----------|---------------|---------------|
| `baseUrl` | `http://localhost:8080` | `http://localhost:8080` |
| `jwtToken` | (empty) | (will be set after login) |

### Pre-request Script (for authenticated requests)
Add this script to requests that require authentication:
```javascript
pm.request.headers.add({
    key: 'Authorization',
    value: 'Bearer ' + pm.environment.get('jwtToken')
});
```

### Test Script (for login request)
Add this script to the login request to automatically save the token:
```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.environment.set("jwtToken", jsonData.token);
    console.log("Token saved to environment variable");
}
```

---

## 13. Testing Workflow

### Step 1: Register a User
1. Use **POST** `/api/auth/register` to create a new account
2. Note the email used for registration

### Step 2: Login
1. Use **POST** `/api/auth/login` with the registered credentials
2. Save the JWT token from the response (or use Postman test script)

### Step 3: Browse Movies and Shows (Public)
1. Use **GET** `/api/movies` to see available movies
2. Use **GET** `/api/shows` to see available shows
3. Use **GET** `/api/shows/{showId}/seats` to see seat availability

### Step 4: Create Booking (Customer)
1. Use **POST** `/api/bookings/validate` to validate and lock seats
2. Use **POST** `/api/bookings` to create the booking

### Step 5: Admin Operations (Admin Role Required)
1. Login with an admin account
2. Use admin endpoints to manage movies, halls, and shows

---

## 14. Notes

- **JWT Token Expiration:** Tokens may expire after a certain period. Re-login if you receive 401 errors.
- **Seat Locking:** Seats are locked for 10 minutes after validation. Complete the booking within this time.
- **Date Formats:** Use ISO 8601 format for dates (`YYYY-MM-DD`) and times (`HH:mm:ss`).
- **Role-Based Access:** 
  - Public endpoints: No authentication required
  - Customer endpoints: Require CUSTOMER role
  - Admin endpoints: Require ADMIN role
- **CORS:** The API allows requests from `http://localhost:4200` (configured for Angular frontend).

---

## 15. Quick Reference

| Endpoint | Method | Auth | Role |
|----------|--------|------|------|
| `/api/auth/register` | POST | No | - |
| `/api/auth/login` | POST | No | - |
| `/api/movies` | GET | No | - |
| `/api/movies/{id}` | GET | No | - |
| `/api/movies/now-showing` | GET | No | - |
| `/api/movies/upcoming` | GET | No | - |
| `/api/halls` | GET | No | - |
| `/api/halls/{id}` | GET | No | - |
| `/api/halls/active` | GET | No | - |
| `/api/shows` | GET | No | - |
| `/api/shows/{id}` | GET | No | - |
| `/api/shows/movie/{movieId}` | GET | No | - |
| `/api/shows?movieId={id}&date={date}` | GET | No | - |
| `/api/shows/{showId}/seats` | GET | No | - |
| `/api/bookings/validate` | POST | Yes | Any |
| `/api/bookings` | POST | Yes | CUSTOMER |
| `/api/admin` | GET | Yes | ADMIN |
| `/api/admin/movies` | GET/POST | Yes | ADMIN |
| `/api/admin/movies/{id}` | GET/PUT/DELETE | Yes | ADMIN |
| `/api/admin/halls` | GET/POST | Yes | ADMIN |
| `/api/admin/halls/{id}` | GET/PUT/DELETE | Yes | ADMIN |
| `/api/admin/halls/{hallId}/seat-layout` | POST | Yes | ADMIN |
| `/api/admin/shows` | GET/POST | Yes | ADMIN |
| `/api/admin/shows/{id}` | GET/PUT/DELETE | Yes | ADMIN |

---

**Last Updated:** 2024
**API Version:** 1.0
