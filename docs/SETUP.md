# Setup Guide

## Prerequisites

- Java 21
- PostgreSQL server
- Git
- Maven is optional because Maven wrapper scripts are included.

## Local Development Setup

1. Create a PostgreSQL database:

```sql
CREATE DATABASE hamro_chalachitraghar_db;
```

2. Review database settings in `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db
    username: postgres
    password: postgres
```

3. Install dependencies and compile:

```powershell
.\mvnw.cmd clean compile
```

4. Run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

5. Check health:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected response:

```json
{ "status": "UP" }
```

## Environment Variables

No environment variables are currently wired in code or configuration.

Current hardcoded configuration:

| Setting | Current value |
| --- | --- |
| Database URL | `jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db` |
| Database username | `postgres` |
| Database password | `postgres` |
| JWT secret | Hardcoded in `JwtUtil` |
| JWT expiration | 1 hour |
| Allowed CORS origin | `http://localhost:4200` |

Unknown / needs confirmation: intended production secret management and environment-specific configuration.

## Install Commands

Windows:

```powershell
.\mvnw.cmd clean compile
```

Unix-like shells:

```bash
./mvnw clean compile
```

## Run Commands

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Unix-like shells:

```bash
./mvnw spring-boot:run
```

## Build Commands

Windows:

```powershell
.\mvnw.cmd clean package
```

Unix-like shells:

```bash
./mvnw clean package
```

The packaged application is produced under `target/`.

## Test Commands

Windows:

```powershell
.\mvnw.cmd test
```

Unix-like shells:

```bash
./mvnw test
```

Current test coverage is limited to a Spring context load test.

## Common Troubleshooting

### PostgreSQL Connection Fails

- Confirm PostgreSQL is running.
- Confirm database `hamro_chalachitraghar_db` exists.
- Confirm username/password in `application.yaml`.
- Confirm port `5432` is available.

### Application Starts But Tables Are Missing

Hibernate is configured with `ddl-auto: update`, so tables should be created/updated at startup. If not, check database permissions for the configured user.

### Login Works But Admin Endpoints Return 403

Registration always creates users with role `CUSTOMER`. No API for creating an admin user exists. An admin user must be inserted or updated directly in the database unless another process exists outside this repository.

### Show Creation Fails

Check all implemented preconditions:

- Movie exists and has status `NOW_SHOWING`.
- Hall exists and is not `INACTIVE`.
- Hall has seat templates generated.
- Show date is in the future.
- Show time does not overlap another non-cancelled show in the same hall and date.

### Booking Fails After Validation

The current implementation locks seats during validation, but `createBooking` rejects currently locked seats. This behavior may prevent the intended validate-then-create flow from succeeding. Needs confirmation and likely a code fix.

### CORS Errors

Only `http://localhost:4200` is allowed. Other frontend origins will be blocked unless CORS configuration is changed.
