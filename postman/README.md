# Hamro Chalchitraghar API - Postman Workspace

This directory contains the Postman workspace used to develop, test, and validate the Hamro Chalchitraghar Backend REST API.

The collection is organized by application modules and mirrors the backend API structure.

---

# Contents

```text
postman/
├── collections/
│   └── Hamro Chalchitraghar API.postman_collection.json
├── environments/
│   └── Local.postman_environment.json
└── README.md
```

---

# Requirements

Before using the collection, ensure the following are available:

- Java 21
- Spring Boot Backend
- PostgreSQL
- Local environment configured
- Postman Desktop

Start the backend:

```bash
./mvnw spring-boot:run
```

Default API URL:

```text
http://localhost:8080
```

---

# Import into Postman

1. Import the collection from:

```
postman/collections/
```

2. Import the environment from:

```
postman/environments/
```

3. Select the **Local** environment.

---

# Environment Variables

| Variable | Description |
| ---------- | ----------- |
| base_url | Backend API URL |
| admin_token | JWT token for Admin |
| staff_token | JWT token for Staff |
| customer_token | JWT token for Customer |
| movie_id | Created movie ID |
| hall_id | Created hall ID |
| show_id | Created show ID |
| seat_id_1 | First available seat |
| seat_id_2 | Second available seat |
| booking_id | Created booking ID |
| report_start_date | Inclusive reporting start date |
| report_end_date | Inclusive reporting end date |
| report_currency | Optional three-letter reporting currency |

The ADMIN Reporting folder includes Reporting 1 dashboards plus daily/weekly/monthly revenue,
booking trends, occupancy, and movie/hall/show performance examples. Performance requests show
pagination and deterministic sorting parameters.

---

# Demo Users

## Administrator

| Field | Value |
| ------ | ----- |
| Email | admin@hamrochalachitraghar.com |
| Password | Admin@123 |

## Staff

| Field | Value |
| ------ | ----- |
| Email | staff@hamrochalachitraghar.com |
| Password | Staff@123 |

## Customer

Customer accounts are created through the Register endpoint.

Example:

```json
{
    "name": "Aarav Sharma",
    "email": "aarav@example.com",
    "password": "Customer@123"
}
```

---

# Demo Resources

## Movie

Jatra

- Genre: Comedy
- Language: Nepali
- Duration: 125 minutes
- Status: NOW_SHOWING

## Hall

AUD1

- Capacity: 188
- Layout: STANDARD
- Status: ACTIVE

## Demo Show

Date

```
2026-08-20
```

Time

```
18:15 - 21:16
```

---

# Collection Structure

```text
Hamro Chalchitraghar API
│
├── 00 Health
├── 01 Authentication
├── 02 Public
│   ├── Movies
│   ├── Halls
│   ├── Shows
│   └── Seats
├── 03 Customer
│   ├── Profile
│   └── Bookings
├── 04 Staff
│   └── Bookings
└── 05 Admin
    ├── Movies
    ├── Halls
    ├── Shows
    └── Users
```

---

# Authentication

Authentication requests automatically store JWT tokens into the selected environment.

| Request | Variable Updated |
| -------- | ---------------- |
| Login - Admin | admin_token |
| Login - Staff | staff_token |
| Login - Customer | customer_token |

Protected folders automatically use their corresponding token.

---

# Recommended Execution Order

Run requests in the following order when testing from a fresh database.

1. Health Check
2. Register Customer
3. Login - Admin
4. Login - Staff
5. Login - Customer
6. Create Movie
7. Create Hall
8. Generate Seat Layout
9. Create Show
10. Get Show Seats
11. Hold Seats
12. Create Booking
13. Confirm Booking
14. Get My Bookings
15. Staff Get Booking By ID

---

# Automatic Variables

Several requests automatically save resource IDs for subsequent requests.

| Request | Variable |
| -------- | -------- |
| Create Movie | movie_id |
| Create Hall | hall_id |
| Create Show | show_id |
| Get Show Seats | seat_id_1, seat_id_2 |
| Create Booking | booking_id |

No manual copy-pasting of IDs is required during the testing workflow.

---

# Notes

- All requests use `{{base_url}}`.
- Collection authorization is inherited through folders.
- Resource IDs are managed automatically through Postman scripts.
- The collection is intended for local development and API verification.
- Future environments (AWS, Production) can be added without modifying request URLs.

---

# Future Enhancements

The Postman workspace will be extended as the backend evolves.

Planned additions include:

- Payment Gateway APIs
- QR Ticket APIs
- Email APIs
- Password Reset APIs
- Ticket Validation APIs
- Redis Cache Testing
- Rate Limiting Tests
- Production (AWS) Environment

---

**Project:** Hamro Chalchitraghar Backend

**Backend:** Java 21 · Spring Boot · PostgreSQL · JWT · Flyway · Docker

**Collection Version:** MVP v1.0
# Reporting 3

The Admin/Reporting collection includes CSV/XLSX exports, period and entity comparisons, and scheduled-report create/list/update/enable/disable/manual-run examples.
