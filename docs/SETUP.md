# Setup Guide

This guide explains how to run Hamro Chalchitraghar Backend locally, with Docker, and in tests.

## Requirements

| Tool | Version / note |
| --- | --- |
| Java | 21 |
| Maven | Maven Wrapper included as `mvnw` and `mvnw.cmd` |
| PostgreSQL | 16 recommended |
| Docker | Required only for containerized run |
| Docker Compose | Required for `docker compose up` |

## Java

Verify Java:

```bash
java -version
```

The project uses Java 21 through the Maven property:

```xml
<java.version>21</java.version>
```

## PostgreSQL

For local development, create a PostgreSQL database matching the development profile default:

```sql
CREATE DATABASE hamro_chalachitraghar_db;
```

Default development connection:

```text
jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db
```

## Maven

Use the Maven Wrapper:

```bash
./mvnw clean test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
```

## Environment Variables

Copy `.env.example` and set values appropriate for your machine:

```text
DB_URL=jdbc:postgresql://localhost:5432/hamro_chalachitraghar_db
DB_USERNAME=postgres
DB_PASSWORD=your_password_here
JWT_SECRET=replace_with_at_least_32_characters_secret
JWT_EXPIRATION_MS=3600000
CORS_ALLOWED_ORIGINS=http://localhost:4200
SPRING_PROFILES_ACTIVE=dev
```

| Variable | Description |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev`, `prod`, or `test` |
| `DB_URL` | JDBC URL |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | HS256 signing secret |
| `JWT_EXPIRATION_MS` | Token expiration in milliseconds |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins |

## Running Locally

Start PostgreSQL, then run:

```bash
./mvnw spring-boot:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts at:

```text
http://localhost:8080
```

Health check:

```text
GET http://localhost:8080/api/public/health
```

## Running with Docker

Build and run the backend with PostgreSQL:

```bash
docker compose up --build
```

Services:

| Service | Container | Port |
| --- | --- | --- |
| PostgreSQL | `hamro-chalchitraghar-postgres` | `5432` |
| Backend | `hamro-chalchitraghar-backend` | `8080` |

Stop containers:

```bash
docker compose down
```

Stop containers and remove database volume:

```bash
docker compose down -v
```

## Running Tests

Run all tests:

```bash
./mvnw test
```

Run a single test class:

```bash
./mvnw -Dtest=BookingApiIntegrationTest test
```

The `test` profile uses:

| Setting | Value |
| --- | --- |
| Database | H2 in-memory |
| H2 mode | PostgreSQL compatibility |
| Hibernate DDL | `create-drop` |
| Flyway | Disabled |
| Security | Real JWT and Spring Security filter chain |

## Swagger URL

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

OpenAPI JSON:

```text
http://localhost:8080/v3/api-docs
```

## Flyway

Flyway is enabled for non-test profiles and reads migrations from:

```text
src/main/resources/db/migration
```

Application startup sequence:

1. Connect to the configured datasource.
2. Apply pending Flyway migrations.
3. Validate JPA mappings against the database schema.
4. Start the web server.

Current migrations:

| Order | Purpose |
| --- | --- |
| 1 | Create users |
| 2 | Create movies |
| 3 | Create halls |
| 4 | Create seat templates |
| 5 | Create shows |
| 6 | Create seats |
| 7 | Create bookings |
| 8 | Create booking seats |
| 9 | Add seat lock owner |

## Troubleshooting

| Problem | Check |
| --- | --- |
| App cannot connect to database | Verify `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and PostgreSQL is running |
| Flyway migration fails | Check SQL syntax and whether the database already contains conflicting objects |
| Hibernate validation fails | Compare entity mappings with Flyway schema |
| JWT requests return `401 Invalid token` | Verify token format and `JWT_SECRET` consistency |
| Protected route returns `403 Access denied` | Verify user role matches route prefix |
| Docker app exits on startup | Inspect logs with `docker compose logs app` |
| PostgreSQL container unhealthy | Inspect logs with `docker compose logs postgres` |
| Port already in use | Change host port mapping in `docker-compose.yml` or stop the conflicting process |
