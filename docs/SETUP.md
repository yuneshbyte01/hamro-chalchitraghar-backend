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
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@hamrochalachitraghar.com
MAIL_ENABLED=false
PASSWORD_RESET_OTP_EXPIRATION_MINUTES=10
PASSWORD_RESET_MAX_ATTEMPTS=5
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
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
| `MAIL_HOST` | SMTP host for password reset email |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password or app password |
| `MAIL_FROM` | From address for password reset email |
| `MAIL_ENABLED` | `false` disables SMTP delivery in dev/test; `true` sends through SMTP |
| `PASSWORD_RESET_OTP_EXPIRATION_MINUTES` | Password reset OTP validity window |
| `TICKET_QR_ENCRYPTION_KEY` | Required Base64 value decoding to exactly 32 bytes; AES-256-GCM ticket QR key |
| `TICKET_QR_KEY_ID` | QR encryption key identifier, default `qr-key-v1`; retained for future rotation |
| `TICKET_QR_TOKEN_VERSION` | Opaque QR token format version, default `1` |
| `TICKET_QR_IMAGE_SIZE` | On-demand square PNG size, default `400` |
| `TICKET_QR_IMAGE_MARGIN` | ZXing QR quiet-zone margin, default `2` |
| `TICKET_ENTRY_WINDOW_MINUTES` | Minutes before show start when online check-in opens, default `60` |
| `TICKET_POST_SHOW_GRACE_MINUTES` | Minutes after show end when online check-in closes, default `30` |
| `TICKET_EXPIRY_RECONCILIATION_INTERVAL_MS` | Issued-ticket expiry job delay, default `60000` |
| `TICKET_EXPIRY_BATCH_SIZE` | Maximum tickets reconciled per expiry run, default `100` |
| `TICKET_PDF_ENABLED` | Enables in-memory PDF downloads/attachments |
| `TICKET_EMAIL_ENABLED` | Enables ticket issuance email attempts; disabling never blocks issuance |
| `TICKET_EMAIL_FROM` | Required production sender for ticket delivery |
| `TICKET_EMAIL_MAX_ATTEMPTS` | Maximum delivery attempts, default `3` |
| `TICKET_EMAIL_RETRY_INTERVAL_MS` | Failed/pending delivery retry delay, default `60000` |
| `TICKET_EMAIL_RETRY_BATCH_SIZE` | Delivery retry batch size, default `50` |

Generate and manage `TICKET_QR_ENCRYPTION_KEY` as a deployment secret; never commit a production key. Production
startup fails if it is absent, invalid Base64, or not exactly 32 bytes. Changing the key without a future key-ring
migration makes existing QR ciphertext unreadable. The committed test/development default is non-production only.
| `PASSWORD_RESET_MAX_ATTEMPTS` | Maximum failed OTP verification attempts |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID used as the expected ID token audience |
| `GOOGLE_CLIENT_SECRET` | Google OAuth client secret; loaded from configuration and never hardcoded |

For local development, `MAIL_ENABLED` defaults to `false`; forgot-password still creates the reset OTP and logs the OTP only under the `dev` profile. In production, configure SMTP credentials and keep `MAIL_ENABLED=true`.

Google Sign-In verifies frontend-provided Google ID tokens against `GOOGLE_CLIENT_ID`. Keep both Google values out of source control and configure separate OAuth clients for development and production when needed.

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
| Mail | Disabled; email service can be mocked in integration tests |

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
## eSewa Sandbox

Enable with the `ESEWA_*` variables in `.env.example`. Official sandbox merchant code is `EPAYTEST`. Published test wallet IDs are `9711111111`, `9711111112`, `9711111113`, and `9711111114`; password `Nepal@123`, token `123456`, and MPIN `1122`. These customer credentials are for sandbox testing only and must never be production configuration. Production requires merchant-issued product code and secret.
Payment operations use `PAYMENT_RECONCILIATION_INTERVAL_MS` (default `300000`), `PAYMENT_RECONCILIATION_BATCH_SIZE` (default `100`), and `PAYMENT_EXPIRY_INTERVAL_MS` (default `60000`). Production deployments should tune these values conservatively and monitor manual-review and consistency endpoints.
