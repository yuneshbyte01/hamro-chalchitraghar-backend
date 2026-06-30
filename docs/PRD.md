# Product Requirements Document

## Product Overview

Hamro Chalchitraghar is a cinema booking backend API. The implemented product supports movie, hall, show, seat, user, and booking management for a cinema ticketing workflow.

The backend exposes public read APIs for browsing movies, halls, shows, and show seats. Registered users can authenticate with JWT and create, confirm, view, and cancel bookings. Admin users can manage movies, halls, shows, hall seat layouts, and users.

## Target Users

- Customers: browse movies and shows, inspect seats, create and manage their own bookings.
- Admins: manage movie catalog, cinema halls, show schedules, generated hall seat layouts, and user records.
- Staff: represented as a role in the code and allowed to view booking details in service logic, but no staff-specific routes are implemented.
- Frontend application: expected local origin is `http://localhost:4200`.

## Core Features

- User registration with default `CUSTOMER` role.
- JWT login and token refresh.
- Public movie browsing by all movies, now showing, and upcoming.
- Public hall browsing by all halls and active halls.
- Public show browsing by all shows, show ID, movie ID, and movie/date.
- Public seat availability lookup for a show.
- Seat validation and 10-minute seat locking.
- Booking creation, confirmation, cancellation, detail lookup, and current user's booking history.
- Admin movie CRUD with soft delete to `ENDED`.
- Admin hall CRUD with soft delete to `INACTIVE`.
- Admin show CRUD with soft delete to `CANCELLED`.
- Admin hall seat layout generation.
- Admin user listing and user detail lookup.
- Centralized API error responses.

## User Flows

### Customer Registration And Login

1. Customer submits name, email, and password to `POST /api/auth/register`.
2. Backend stores a bcrypt-hashed password and assigns role `CUSTOMER`.
3. Customer logs in with `POST /api/auth/login`.
4. Backend returns a JWT containing email and role claims.

### Browse And Book Seats

1. Customer browses movies with `GET /api/movies` or status-specific movie endpoints.
2. Customer browses shows with `GET /api/shows`, `GET /api/shows/movie/{movieId}`, or `GET /api/shows?movieId={id}&date={date}`.
3. Customer views seats for a show with `GET /api/shows/{showId}/seats`.
4. Customer validates selected seats with `POST /api/bookings/validate`.
5. Backend locks available selected seats for 10 minutes.
6. Customer creates a booking with `POST /api/bookings`.
7. Backend creates an `INITIATED` booking and marks seats `RESERVED`.
8. Customer confirms with `POST /api/bookings/{bookingId}/confirm`.
9. Backend marks seats `BOOKED` and booking `CONFIRMED`.

### Customer Booking Management

1. Customer lists their bookings with `GET /api/bookings/my`.
2. Customer views a booking with `GET /api/bookings/{bookingId}`.
3. Customer cancels an `INITIATED` booking before show time with `POST /api/bookings/{bookingId}/cancel`.
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
- The system shall issue stateless JWTs with 1-hour expiration.
- The system shall allow public read access to movies, halls, shows, and show seats.
- The system shall restrict `/api/admin/**` to users with `ADMIN` role.
- The system shall require authentication for `/api/bookings/**`.
- The system shall allow only a booking owner, admin, or staff to view a booking by ID according to service logic.
- The system shall reject duplicate seat IDs during booking creation.
- The system shall use pessimistic write locks when selecting seats for booking operations.
- The system shall reject booking seats that are already booked, reserved, or currently locked.
- The system shall generate 188 seat templates for a hall: row A seats 1-8 as `PREMIUM`, rows B-J seats 1-20 as `PLATINUM`.
- The system shall generate show seats from hall seat templates when a show is created.
- The system shall assign seat prices by type: `PREMIUM` = 750.0, `PLATINUM` = 500.0.
- The system shall reject show creation or updates for inactive halls or movies not in `NOW_SHOWING`.
- The system shall reject overlapping shows in the same hall on the same date.

## Non-Functional Requirements

- Security: JWT-based stateless authentication and role-based authorization are implemented.
- Data integrity: JPA constraints, unique hall names, unique user emails, and pessimistic locks are used.
- Reliability: transaction boundaries exist on service operations that mutate related records.
- Maintainability: code follows a layered Controller -> Service -> Repository structure.
- API usability: validation annotations and centralized error handling produce consistent error responses.
- Performance: database indexes exist on hall name and hall status; repository queries support common lookups.

## Assumptions And Open Questions

- Unknown / needs confirmation: payment flow is not implemented.
- Unknown / needs confirmation: email/SMS ticket delivery is not implemented.
- Unknown / needs confirmation: production deployment target is not present in the repository.
- Unknown / needs confirmation: admin account creation process is not implemented through an API.
- Unknown / needs confirmation: staff-facing workflows are not exposed as routes.
- Unknown / needs confirmation: frontend repository and its production origin are not included.
- Unknown / needs confirmation: booking lifecycle after `INITIATED` and `CONFIRMED` does not include payment state.
- Unknown / needs confirmation: business rules for `PENDING`, `BOOKED`, `EXPIRED`, `RESERVED`, `CANCELLED`, `RUNNING`, and `COMPLETED` enum values beyond implemented code.
