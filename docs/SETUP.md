# Setup Guide

## Prerequisites

- Java 21
- PostgreSQL server
- Git
- Maven is optional because Maven wrapper scripts are included.

## Profiles

The active profile is controlled by:

```text
SPRING_PROFILES_ACTIVE
```

Default:

```text
dev
```

Profiles:

- `dev`: local development defaults with environment variable overrides.
- `prod`: production profile requiring environment variables.
- `test`: integration test profile using H2 in PostgreSQL compatibility mode.

## Environment Variables

Use `.env.example` as the template:

```text
DB_URL=jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
JWT_SECRET=replace_with_at_least_32_characters_secret
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:4200
SPRING_PROFILES_ACTIVE=dev
```

Do not commit real secrets. `.env` is ignored by Git.

## PostgreSQL Setup

Create the local database:

```sql
CREATE DATABASE hamro_chalachitraghar_db;
```

Create or choose a PostgreSQL user with permission to connect and create/update objects in that database.

Example local environment values:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password_here"
$env:JWT_SECRET="replace_with_at_least_32_characters_secret"
$env:JWT_EXPIRATION_MS="3600000"
$env:CORS_ALLOWED_ORIGINS="http://localhost:4200"
$env:SPRING_PROFILES_ACTIVE="dev"
```

## Local Development

Compile:

```powershell
.\mvnw.cmd clean compile
```

Run:

```powershell
.\mvnw.cmd spring-boot:run
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/api/public/health
```

Expected response:

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

## Production Profile

Set:

```text
SPRING_PROFILES_ACTIVE=prod
```

Required environment variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`

Optional:

- `JWT_EXPIRATION_MS`, default `3600000`.

The production profile does not provide database or secret defaults.

## Test Commands

Run all tests:

```powershell
.\mvnw.cmd test
```

Run one test class:

```powershell
.\mvnw.cmd -Dtest=AuthorizationAndErrorApiIntegrationTest test
```

Test configuration:

- File: `src/test/resources/application-test.yaml`
- Database: H2 in PostgreSQL compatibility mode
- Flyway: disabled
- Hibernate: `create-drop`
- Profile: `test`

Tests do not require a local PostgreSQL database.

## Build Commands

Create a package:

```powershell
.\mvnw.cmd clean package
```

The packaged artifact is written under `target/`.

## Flyway Migrations

Flyway runs at application startup for `dev` and `prod`.

Migration files live under:

```text
src/main/resources/db/migration
```

To add a migration:

1. Find the latest migration version.
2. Add a new file using the next version number:

```text
V10__description.sql
```

3. Use PostgreSQL SQL.
4. Run:

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd test
```

Do not edit already-applied migrations in shared environments.

## Common Troubleshooting

### PostgreSQL Connection Fails

- Confirm PostgreSQL is running.
- Confirm `hamro_chalachitraghar_db` exists.
- Confirm `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- Confirm port `5432` is available.

### Flyway Fails

- Check the migration error in logs.
- Confirm the connected database is the expected database.
- Confirm migrations are ordered correctly.
- Do not modify old migrations after they have been applied.

### Hibernate Validation Fails

Hibernate uses `ddl-auto: validate`.

If validation fails:

- Check that all Flyway migrations ran.
- Check that the schema matches the entity mappings.
- Add a new migration for schema changes.

### Login Works But Admin Endpoints Return 403

Registration creates `CUSTOMER` users only.

There is no admin or staff creation endpoint yet. Admin or staff users must be created through trusted database setup or another operational process.

### Show Creation Fails

Check these conditions:

- Movie exists and has status `NOW_SHOWING`.
- Hall exists and is not `INACTIVE`.
- Hall has seat templates generated through `POST /api/admin/halls/{hallId}/seat-layout`.
- Show date is in the future.
- Show time does not overlap another non-cancelled show in the same hall and date.

### Booking Fails

Check these conditions:

- Seat IDs are not duplicated.
- Seats belong to the requested show.
- Seats are `AVAILABLE` or held by the same user.
- Seats are not `RESERVED` or `BOOKED`.
- Another user does not currently own the active seat hold.

### CORS Errors

Set:

```text
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

Multiple origins can be comma-separated.
