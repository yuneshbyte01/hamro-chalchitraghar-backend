# Product Requirements Document

## Product Overview

Hamro Chalchitraghar is a cinema booking backend API. The implemented V2 product supports movie, hall, show, seat, user, and booking management for a cinema ticketing workflow.

The backend exposes public read APIs for browsing movies, halls, shows, and show seats. Registered users authenticate with JWT and can hold seats, create bookings, confirm bookings, view bookings, and cancel initiated bookings. Admin users can manage movies, halls, shows, hall seat layouts, and users.

## Target Users

- Customers: browse movies and shows, inspect seats, hold seats, and manage their own bookings.
- Admins: manage movie catalog, halls, show schedules, generated hall seat layouts, and user records.
- Staff: can view booking details through the implemented staff booking endpoint.
- Frontend application: expected local origin is configurable, commonly `http://localhost:4200`.

## Core Features

- User registration with default `CUSTOMER` role.
- JWT login and token refresh.
- Public movie browsing by all movies, now showing, and upcoming.
- Public hall browsing by all halls and active halls.
- Public show browsing by all shows, show ID, movie ID, and movie/date.
- Public seat availability lookup for a show.
- Seat hold flow with 10-minute locks and lock ownership.
- Booking creation, confirmation, cancellation, detail lookup, and current user's booking history.
- Staff booking detail lookup.
- Admin movie CRUD with soft delete to `ENDED`.
- Admin hall CRUD with soft delete to `INACTIVE`.
- Admin show CRUD with soft delete to `CANCELLED`.
- Admin hall seat layout generation.
- Admin user listing and user detail lookup.
- Standard API response wrapper for success and error responses.

## User Flows

### Customer Registration And Login

1. Customer submits name, email, and password to `POST /api/auth/register`.
2. Backend stores a BCrypt-hashed password and assigns role `CUSTOMER`.
3. Customer logs in with `POST /api/auth/login`.
4. Backend returns a JWT containing email and role claims.

### Browse And Book Seats

1. Customer browses movies with `GET /api/public/movies`.
2. Customer browses shows with `GET /api/public/shows`.
3. Customer views seats with `GET /api/public/shows/{showId}/seats`.
4. Customer holds seats with `POST /api/customer/bookings/hold`.
5. Backend locks available selected seats for 10 minutes and records `locked_by_user_id`.
6. Customer creates a booking with `POST /api/customer/bookings`.
7. Backend creates an `INITIATED` booking and marks seats `RESERVED`.
8. Customer confirms with `POST /api/customer/bookings/{bookingId}/confirm`.
9. Backend marks seats `BOOKED` and booking `CONFIRMED`.

### Customer Booking Management

1. Customer lists their bookings with `GET /api/customer/bookings/my`.
2. Customer views a booking with `GET /api/customer/bookings/{bookingId}`.
3. Customer cancels an `INITIATED` booking before show time with `POST /api/customer/bookings/{bookingId}/cancel`.
4. Backend marks booking `CANCELLED` and seats `AVAILABLE`.

### Admin Catalog And Schedule Management

1. Admin logs in and receives a JWT with role `ADMIN`.
2. Admin creates movies and marks relevant movies `NOW_SHOWING`.
3. Admin creates halls.
4. Admin generates a hall seat layout with `POST /api/admin/halls/{hallId}/seat-layout`.
5. Admin creates shows for `NOW_SHOWING` movies in active halls.
6. Backend rejects overlapping shows for the same hall and date.
7. Backend generates show seats from the hall seat template.

## Functional Requirements

- The system shall allow public registration and login.
- The system shall hash user passwords with BCrypt.
- The system shall issue stateless JWTs with configurable expiration.
- The system shall allow public read access to movies, halls, shows, and show seats under `/api/public/**`.
- The system shall restrict `/api/admin/**` to users with `ADMIN` role.
- The system shall restrict `/api/staff/**` to users with `STAFF` or `ADMIN` role.
- The system shall require authentication for `/api/customer/**`.
- The system shall allow only a booking owner, admin, or staff to view a booking by ID according to service logic.
- The system shall reject duplicate seat IDs during hold and booking creation.
- The system shall use pessimistic write locks when selecting seats for booking-sensitive operations.
- The system shall reject booking seats that are already booked, reserved, or actively locked by another user.
- The system shall generate 188 seat templates for a hall: row A seats 1-8 as `PREMIUM`, rows B-J seats 1-20 as `PLATINUM`.
- The system shall generate show seats from hall seat templates when a show is created.
- The system shall assign seat prices by type: `PREMIUM` = 750.0, `PLATINUM` = 500.0.
- The system shall reject show creation or updates for inactive halls or movies not in `NOW_SHOWING`.
- The system shall reject overlapping shows in the same hall on the same date.
- The system shall return standard `ApiResponse` bodies for success and handled errors.

## Non-Functional Requirements

- Security: JWT-based stateless authentication and role-based authorization.
- Data integrity: Flyway migrations, JPA constraints, unique hall names, unique user emails, service validations, and pessimistic locks.
- Reliability: transaction boundaries exist on service operations that mutate related records.
- Maintainability: modular monolith package structure with Controller -> Service -> Repository flow.
- API usability: validation annotations and centralized error handling produce consistent response shapes.
- Testability: core behavior is covered with Spring Boot MockMvc integration tests.

## Partial Or Planned Features

- Staff booking detail is implemented.
- Staff check-in is planned, not implemented.
- Admin staff creation is planned, not implemented.
- Payment flow is not implemented.
- Email/SMS ticket delivery is not implemented.
- Docker support and GitHub Actions test CI are implemented.
- Production deployment automation is not implemented.
- Booking lifecycle after `INITIATED` and `CONFIRMED` does not include payment state.
- Some enum values exist for future workflows beyond currently implemented services.
