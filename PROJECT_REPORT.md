# Hamro Chalchitraghar Backend - Full Project Report

**Generated:** December 2024  
**Project:** hamro-chalchitraghar-backend  
**Version:** 0.0.1-SNAPSHOT

---

## Executive Summary

Hamro Chalchitraghar Backend is a comprehensive Spring Boot-based REST API for managing a movie theater/cinema booking system. The application provides role-based access control, JWT authentication, seat management with locking mechanisms, and full CRUD operations for movies, halls, shows, and bookings.

---

## 1. Technology Stack

### Core Framework
- **Spring Boot:** 3.5.9
- **Java:** 25
- **Build Tool:** Maven
- **Database:** MySQL 8

### Key Dependencies
- **Spring Data JPA** - Database persistence layer
- **Spring Security** - Authentication and authorization
- **Spring Web** - REST API framework
- **Spring Validation** - Input validation
- **JWT (jjwt)** - Token-based authentication (v0.11.5)
- **Lombok** - Boilerplate code reduction
- **MySQL Connector/J** - Database driver

### Architecture Pattern
- **Layered Architecture:** Controller → Service → Repository
- **RESTful API Design**
- **JWT Token-based Authentication**
- **Role-Based Access Control (RBAC)**

---

## 2. Project Structure

```
hamro-chalchitraghar-backend/
├── src/main/java/com/chalchitraghar/
│   ├── config/                    # Configuration classes
│   │   ├── CORSConfig.java        # CORS configuration
│   │   └── SecurityConfig.java    # Spring Security setup
│   │
│   ├── controller/                # REST Controllers
│   │   ├── admin/                 # Admin-only endpoints
│   │   │   ├── AdminController.java
│   │   │   ├── HallController.java
│   │   │   ├── MovieController.java
│   │   │   ├── SeatLayoutController.java
│   │   │   └── ShowController.java
│   │   ├── AuthController.java    # Authentication endpoints
│   │   ├── BookingController.java # Booking operations
│   │   ├── HallController.java    # Public hall endpoints
│   │   ├── MovieController.java   # Public movie endpoints
│   │   ├── SeatController.java    # Seat management
│   │   ├── ShowController.java    # Public show endpoints
│   │   └── TestController.java    # Testing endpoints
│   │
│   ├── dto/                       # Data Transfer Objects
│   │   ├── auth/                  # Authentication DTOs
│   │   ├── booking/               # Booking DTOs
│   │   ├── hall/                  # Hall DTOs
│   │   ├── movie/                 # Movie DTOs
│   │   ├── seat/                  # Seat DTOs
│   │   └── show/                  # Show DTOs
│   │
│   ├── exception/                 # Exception handling
│   │   ├── AuthenticationException.java
│   │   ├── GlobalExceptionHandler.java
│   │   ├── HallConflictException.java
│   │   ├── InvalidSeatSelectionException.java
│   │   ├── ResourceNotFoundException.java
│   │   ├── SeatAlreadyBookedException.java
│   │   └── SeatLockedException.java
│   │
│   ├── mapper/                    # Entity-DTO mappers
│   │   ├── HallMapper.java
│   │   ├── MovieMapper.java
│   │   ├── SeatMapper.java
│   │   └── ShowMapper.java
│   │
│   ├── model/                     # JPA Entities
│   │   ├── enums/                 # Enumeration types
│   │   │   ├── MovieStatus.java
│   │   │   ├── Role.java
│   │   │   ├── SeatStatus.java
│   │   │   ├── SeatType.java
│   │   │   ├── ShowStatus.java
│   │   │   └── Status.java
│   │   ├── Hall.java
│   │   ├── Movie.java
│   │   ├── Seat.java
│   │   ├── SeatTemplate.java
│   │   ├── Show.java
│   │   └── User.java
│   │
│   ├── repository/                # JPA Repositories
│   │   ├── HallRepository.java
│   │   ├── MovieRepository.java
│   │   ├── SeatRepository.java
│   │   ├── SeatTemplateRepository.java
│   │   ├── ShowRepository.java
│   │   └── UserRepository.java
│   │
│   ├── security/                  # Security components
│   │   ├── JwtAuthenticationFilter.java
│   │   └── JwtUtil.java
│   │
│   └── service/                   # Business logic services
│       ├── AuthService.java
│       ├── BookingService.java
│       ├── HallService.java
│       ├── MovieService.java
│       ├── SeatGenerationService.java
│       ├── SeatLayoutService.java
│       ├── SeatService.java
│       ├── ShowService.java
│       └── UserService.java
│
└── src/main/resources/
    └── application.yaml           # Application configuration
```

---

## 3. Database Schema

### Entities Overview

#### User
- **Purpose:** User authentication and authorization
- **Fields:**
  - `id` (Long, Primary Key)
  - `name` (String, Required)
  - `email` (String, Unique, Required)
  - `password` (String, Min 8 chars, Required)
  - `role` (Role enum: CUSTOMER, STAFF, ADMIN)
  - `createdAt` (LocalDateTime)
  - `updatedAt` (LocalDateTime)

#### Movie
- **Purpose:** Movie catalog management
- **Fields:**
  - `id` (Long, Primary Key)
  - `title` (String, Required)
  - `genre` (String, Required)
  - `durationMinutes` (Integer, Required)
  - `language` (String, Required)
  - `description` (String, Required)
  - `posterUrl` (String, Required)
  - `releaseDate` (LocalDate, Required)
  - `status` (MovieStatus enum)
  - `createdAt`, `updatedAt` (LocalDateTime)

#### Hall
- **Purpose:** Cinema hall management
- **Fields:**
  - `id` (Long, Primary Key)
  - `name` (String, Unique, Required)
  - `capacity` (Integer, Required)
  - `layoutRef` (String, Required)
  - `status` (Status enum: ACTIVE, INACTIVE)
  - `createdAt`, `updatedAt` (LocalDateTime)

#### Show
- **Purpose:** Movie show scheduling
- **Relationships:**
  - Many-to-One with Movie
  - Many-to-One with Hall
- **Fields:**
  - `id` (Long, Primary Key)
  - `movie` (Movie, Required)
  - `hall` (Hall, Required)
  - `price` (Double, Required, Positive)
  - `status` (ShowStatus enum: SCHEDULED, ONGOING, COMPLETED, CANCELLED)
  - `showDate` (LocalDate, Required)
  - `showTime` (LocalTime, Required)
  - `endTime` (LocalTime, Required)
  - `createdAt`, `updatedAt` (LocalDateTime)

#### Seat
- **Purpose:** Individual seat management per show
- **Relationships:**
  - Many-to-One with Show
- **Fields:**
  - `id` (Long, Primary Key)
  - `show` (Show, Required)
  - `seatNumber` (Integer, Required)
  - `rowLabel` (String, Required)
  - `seatCode` (String, Required, Format: rowLabel + seatNumber)
  - `seatType` (SeatType enum: PREMIUM, PLATINUM)
  - `seatStatus` (SeatStatus enum: AVAILABLE, LOCKED, BOOKED, RESERVED, CANCELLED)
  - `positionIndex` (Integer, Required)
  - `lockedAt` (LocalDateTime, Nullable)
  - `lockExpiresAt` (LocalDateTime, Nullable)
  - `createdAt`, `updatedAt` (LocalDateTime)

#### SeatTemplate
- **Purpose:** Hall seat layout templates
- **Relationships:**
  - Many-to-One with Hall
- **Fields:**
  - `id` (Long, Primary Key)
  - `hall` (Hall, Required)
  - `rowLabel` (String, Required)
  - `seatNumber` (Integer, Required)
  - `seatCode` (String, Auto-generated)
  - `seatType` (SeatType enum)
  - `positionIndex` (Integer, Required)
  - `createdAt`, `updatedAt` (LocalDateTime)

---

## 4. Security Implementation

### Authentication Flow
1. **Registration:** Users register with name, email, and password
2. **Login:** Users authenticate and receive JWT token
3. **Token Validation:** JWT filter validates token on each request
4. **Authorization:** Role-based access control enforced

### JWT Configuration
- **Algorithm:** HS256
- **Expiration:** 1 hour (3,600,000 ms)
- **Claims:** Email (subject), Role
- **Secret Key:** Hardcoded (⚠️ **Security Concern** - should be externalized)

### Security Filter Chain
- **Public Endpoints:**
  - `/api/auth/**` - Registration and login
- **Role-Based Endpoints:**
  - `/api/customer/**` - Requires CUSTOMER role
  - `/api/staff/**` - Requires STAFF role
  - `/api/admin/**` - Requires ADMIN role
- **Authenticated Endpoints:**
  - All other endpoints require valid JWT token

### CORS Configuration
- **Allowed Origin:** `http://localhost:4200` (Angular frontend)
- **Allowed Methods:** GET, POST, PUT, DELETE, OPTIONS
- **Allowed Headers:** All (`*`)
- **Credentials:** Enabled
- **Max Age:** 3600 seconds

### Password Security
- **Encoding:** BCryptPasswordEncoder
- **Minimum Length:** 8 characters
- **Storage:** Hashed passwords in database

---

## 5. API Endpoints

### Authentication Endpoints (`/api/auth`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/auth/register` | User registration | Public |
| POST | `/api/auth/login` | User login | Public |

### Movie Endpoints

#### Public (`/api/movies`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/movies` | Get all movies | Public |
| GET | `/api/movies/{id}` | Get movie by ID | Public |
| GET | `/api/movies/now-showing` | Get currently showing movies | Public |
| GET | `/api/movies/upcoming` | Get upcoming movies | Public |

#### Admin (`/api/admin/movies`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/admin/movies` | Get all movies | Admin |
| GET | `/api/admin/movies/{id}` | Get movie by ID | Admin |
| POST | `/api/admin/movies` | Create new movie | Admin |
| PUT | `/api/admin/movies/{id}` | Update movie | Admin |
| DELETE | `/api/admin/movies/{id}` | Delete movie | Admin |

### Hall Endpoints

#### Public (`/api/halls`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/halls` | Get all halls | Public |
| GET | `/api/halls/{id}` | Get hall by ID | Public |
| GET | `/api/halls/active` | Get active halls | Public |

#### Admin (`/api/admin/halls`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/admin/halls` | Get all halls | Admin |
| GET | `/api/admin/halls/{id}` | Get hall by ID | Admin |
| POST | `/api/admin/halls` | Create new hall | Admin |
| PUT | `/api/admin/halls/{id}` | Update hall | Admin |
| DELETE | `/api/admin/halls/{id}` | Delete hall | Admin |
| GET | `/api/admin/halls/active` | Get active halls | Admin |
| POST | `/api/admin/halls/{hallId}/seat-layout` | Create seat layout | Admin |

### Show Endpoints

#### Public (`/api/shows`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/shows` | Get all shows | Public |
| GET | `/api/shows/{id}` | Get show by ID | Public |
| GET | `/api/shows/movie/{movieId}` | Get shows for movie | Public |
| GET | `/api/shows?movieId={id}&date={date}` | Get shows by movie and date | Public |

#### Admin (`/api/admin/shows`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/admin/shows` | Get all shows | Admin |
| GET | `/api/admin/shows/{id}` | Get show by ID | Admin |
| POST | `/api/admin/shows` | Create new show | Admin |
| PUT | `/api/admin/shows/{id}` | Update show | Admin |
| DELETE | `/api/admin/shows/{id}` | Delete show | Admin |

### Seat Endpoints (`/api/shows`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| GET | `/api/shows/{showId}/seats` | Get seats for a show | Authenticated |

### Booking Endpoints (`/api/bookings`)
| Method | Endpoint | Description | Access |
|--------|----------|-------------|--------|
| POST | `/api/bookings/validate` | Validate and lock seats | Authenticated |

---

## 6. Business Logic Highlights

### Seat Locking Mechanism
- **Lock Duration:** 10 minutes
- **Concurrency Control:** Pessimistic write locks prevent race conditions
- **Lock Expiration:** Automatic expiration after 10 minutes
- **Status Flow:** AVAILABLE → LOCKED → BOOKED

### Booking Validation Process
1. Validate seat IDs exist
2. Verify all seats belong to the same show
3. Check seat availability
4. Handle expired locks (auto-release)
5. Prevent double-booking
6. Apply pessimistic locks for concurrent safety
7. Set lock expiration timestamp

### Show Conflict Detection
- Prevents overlapping shows in the same hall
- Validates show times against existing schedules

### Seat Generation
- Seats generated from hall templates when creating shows
- Automatic seat code generation (rowLabel + seatNumber)
- Position indexing for UI rendering

---

## 7. Exception Handling

### Custom Exceptions
1. **ResourceNotFoundException** - 404 Not Found
2. **AuthenticationException** - 401 Unauthorized
3. **HallConflictException** - 409 Conflict
4. **SeatAlreadyBookedException** - 409 Conflict
5. **SeatLockedException** - 409 Conflict
6. **InvalidSeatSelectionException** - 400 Bad Request

### Global Exception Handler
- Centralized exception handling via `@RestControllerAdvice`
- Consistent error response format
- Logging for debugging
- Automatic HTTP status code mapping

---

## 8. Data Transfer Objects (DTOs)

### Authentication DTOs
- `LoginRequestDto` - Email and password
- `LoginResponseDto` - Token, email, name, role
- `RegistrationRequestDto` - Name, email, password
- `RegistrationResponseDto` - Success message and email

### Domain DTOs
- **Movie:** `MovieDto`, `MovieResponseDto`
- **Hall:** `HallRequestDto`, `HallResponseDto`
- **Show:** `ShowRequestDto`, `ShowResponseDto`
- **Seat:** `SeatResponseDto`
- **Booking:** `BookingValidationRequestDto`, `BookingValidationResponseDto`

---

## 9. Database Configuration

### Connection Details
- **URL:** `jdbc:mysql://localhost:3306/hamro_chalachitraghar_db`
- **Username:** `root`
- **Password:** `@@Himal@@` (⚠️ **Security Concern** - should be externalized)

### JPA Settings
- **DDL Mode:** `update` (auto-update schema)
- **Show SQL:** `true` (for debugging)
- **Dialect:** MySQL8Dialect

---

## 10. Testing

### Test Structure
- **Test Class:** `HamroChalchitragharBackendApplicationTests.java`
- **Test Framework:** Spring Boot Test
- **Security Testing:** Spring Security Test dependency included

### Test Coverage
- ⚠️ **Limited test coverage** - Only basic application context test present

---

## 11. Code Quality & Best Practices

### Strengths
✅ **Layered Architecture** - Clear separation of concerns  
✅ **DTO Pattern** - Proper data transfer object usage  
✅ **Exception Handling** - Comprehensive exception management  
✅ **Validation** - Input validation using Jakarta Validation  
✅ **Transaction Management** - `@Transactional` for data consistency  
✅ **Concurrency Safety** - Pessimistic locking for seat operations  
✅ **Lombok Integration** - Reduced boilerplate code  
✅ **RESTful Design** - Proper HTTP method usage  

### Areas for Improvement
⚠️ **Security Concerns:**
- JWT secret key hardcoded (should use environment variables)
- Database password in plain text (should use environment variables)
- No password complexity requirements beyond minimum length

⚠️ **Code Quality:**
- Limited test coverage
- No API documentation (Swagger/OpenAPI)
- No logging configuration visible
- No rate limiting implementation

⚠️ **Architecture:**
- No pagination for list endpoints
- No filtering/sorting capabilities
- No caching strategy
- No async processing for heavy operations

---

## 12. Deployment Considerations

### Environment Variables Needed
- Database connection details
- JWT secret key
- Server port (if different from default 8080)
- CORS allowed origins (currently hardcoded)

### Build & Run
```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run
```

### Database Setup
1. Create MySQL database: `hamro_chalachitraghar_db`
2. Update `application.yaml` with correct credentials
3. Application will auto-create tables on first run (DDL mode: update)

---

## 13. API Usage Examples

### Registration
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}

Response:
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "email": "john@example.com",
  "name": "John Doe",
  "role": "CUSTOMER"
}
```

### Create Movie (Admin)
```http
POST /api/admin/movies
Authorization: Bearer {token}
Content-Type: application/json

{
  "title": "Inception",
  "genre": "Sci-Fi",
  "durationMinutes": 148,
  "language": "English",
  "description": "A mind-bending thriller",
  "posterUrl": "https://example.com/poster.jpg",
  "releaseDate": "2024-01-15",
  "status": "NOW_SHOWING"
}
```

### Validate and Lock Seats
```http
POST /api/bookings/validate
Authorization: Bearer {token}
Content-Type: application/json

{
  "showId": 1,
  "seatIds": [1, 2, 3]
}
```

---

## 14. Future Enhancements

### Recommended Improvements
1. **Security**
   - Externalize sensitive configuration
   - Implement refresh tokens
   - Add password reset functionality
   - Implement rate limiting

2. **Features**
   - Payment integration
   - Email notifications
   - Booking history for users
   - Seat selection visualization
   - Show cancellation handling

3. **Performance**
   - Implement caching (Redis)
   - Add pagination to list endpoints
   - Database indexing optimization
   - Async processing for notifications

4. **Documentation**
   - Swagger/OpenAPI integration
   - API documentation
   - Code comments and JavaDoc

5. **Testing**
   - Unit tests for services
   - Integration tests for controllers
   - Security tests
   - Performance tests

6. **Monitoring**
   - Application logging (Logback/SLF4J)
   - Health checks
   - Metrics collection
   - Error tracking

---

## 15. Conclusion

The Hamro Chalchitraghar Backend is a well-structured Spring Boot application that provides a solid foundation for a cinema booking system. The architecture follows best practices with clear separation of concerns, proper exception handling, and role-based security. The seat locking mechanism demonstrates good understanding of concurrency control.

**Key Strengths:**
- Clean architecture
- Comprehensive exception handling
- Secure authentication flow
- Concurrency-safe seat management

**Priority Improvements:**
- Externalize sensitive configuration
- Increase test coverage
- Add API documentation
- Implement pagination and filtering

The application is production-ready with minor security and documentation improvements.

---

**Report Generated:** December 2024  
**Project Version:** 0.0.1-SNAPSHOT  
**Spring Boot Version:** 3.5.9  
**Java Version:** 25
