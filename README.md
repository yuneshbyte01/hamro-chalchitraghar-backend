# Hamro Chalchitraghar Backend

Spring Boot backend for Hamro Chalchitraghar, a cinema ticket booking API for movies, halls, shows, seats, users, and bookings.

This repository is currently on backend V2. V2 uses a modular monolith structure, PostgreSQL, Flyway migrations, JWT authentication, role-based endpoint groups, a seat hold booking flow, standard API responses, and integration tests.

## Tech Stack

- Java 21
- Spring Boot 3.x
- Spring Web
- Spring Security with JWT
- Spring Data JPA and Hibernate
- PostgreSQL
- Flyway database migrations
- Maven wrapper
- JUnit, MockMvc, Spring Security Test, and H2 for tests

## Documentation

- [Product Requirements](docs/PRD.md)
- [Technical Requirements](docs/TRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [API Reference](docs/API.md)
- [Setup Guide](docs/SETUP.md)
- [Database Documentation](docs/DATABASE.md)
- [Maintenance Guide](docs/MAINTENANCE.md)
- [V2 Migration Notes](docs/V2_MIGRATION.md)

## Quick Start

Prerequisites:

- Java 21
- PostgreSQL
- Git

Create a local PostgreSQL database:

```sql
CREATE DATABASE hamro_chalachitraghar_db;
```

Copy `.env.example` to `.env` or set equivalent environment variables:

```text
DB_URL=jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
JWT_SECRET=replace_with_at_least_32_characters_secret
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:4200
SPRING_PROFILES_ACTIVE=dev
```

Run the application on Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```text
GET http://localhost:8080/api/public/health
```

Run tests:

```powershell
.\mvnw.cmd test
```

See [Setup Guide](docs/SETUP.md) for full local setup and troubleshooting.
