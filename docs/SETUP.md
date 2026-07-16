# Setup Guide

## Reporting 1 configuration

Reporting 1 requires no migration, dependency, scheduler, cache, or new environment variable. It
uses existing `app.time-zone`/`APP_TIME_ZONE` (default `Asia/Kathmandu`) to convert inclusive API
dates into local-midnight boundaries. Set the deployment timezone explicitly and keep database
timestamp handling consistent. Existing `/api/admin/**` security provides ADMIN-only access.

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

PostgreSQL must become healthy before the application starts. The application container then checks
`/actuator/health/readiness`; this requires database readiness but not SMTP or eSewa availability.

## Observability configuration

The application includes Actuator and the Prometheus registry. Safe defaults are:

```text
APP_VERSION=local
MANAGEMENT_ENDPOINTS_EXPOSED=health,info,prometheus
HEALTH_SHOW_DETAILS=never
HEALTH_PROBES_ENABLED=true
GRACEFUL_SHUTDOWN_TIMEOUT=30s
```

Health, liveness and readiness are public status-only endpoints. Use an ADMIN bearer token for
`/actuator/info` and `/actuator/prometheus`. Do not expose all Actuator endpoints. Prometheus and Grafana
servers are not part of this phase.

Shutdown is bounded by `GRACEFUL_SHUTDOWN_TIMEOUT`. The notification executor drains for at most 20 seconds;
persisted interrupted deliveries and refund claims remain recoverable through existing retry/stale-recovery flows.

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
# Notification-1 setup

Notification-1 requires no new environment variables, SMTP configuration, async executor, or
scheduler. It is a database-backed in-app feature installed by Flyway migration V26. Customer list
pagination defaults to 20 and is bounded at 100 in the service. Existing password-reset and ticket
email configuration remains unchanged.
# Notification-2 runtime

Business-event notifications are enabled by default and need no environment variables, SMTP
settings, async executor, broker, or new runtime service. They persist IN_APP rows in the existing
database. Password-reset and ticket-delivery email configuration is unchanged.

## General notification email

## Reminders and retention

Reminders and retention default off. Configure their enable flags, ISO-8601 durations, age limits,
and bounded batch sizes with the `NOTIFICATION_*` variables in `.env.example`. `APP_TIME_ZONE`
controls reminder calculations, and tests send no real SMTP.


Set `NOTIFICATION_EMAIL_ENABLED=true` and provide `NOTIFICATION_EMAIL_FROM` plus the existing
`spring.mail`/`MAIL_*` SMTP settings. Pool, queue, retry, backoff, batch, processing-timeout, sender
name, and async settings are listed in `.env.example`. When disabled, startup needs no SMTP
credentials and eligible deliveries are recorded as `SKIPPED`. Tests disable real SMTP and may set
async off for deterministic dispatch.
# Audit-1 setup

Audit-1 requires no new environment variables, broker, executor, AOP, MDC, or tracing setup. It uses
the existing Spring `Clock` and stores bounded validated JSON in PostgreSQL TEXT columns for stable
H2 PostgreSQL-mode tests.
# Audit-2 setup

Automatic audit integration is active by default and adds no environment variables, request-context
filter, MDC, proxy trust, broker, or asynchronous executor.
# Audit-3 configuration

# Audit-4 operations

Configure export with `AUDIT_EXPORT_ENABLED`, `AUDIT_EXPORT_MAX_ROWS`, and
`AUDIT_EXPORT_MAX_RANGE_DAYS`. Reports use `AUDIT_REPORTS_MAX_RANGE_DAYS` and
`AUDIT_REPORTS_DEFAULT_BUCKET`. Retention is opt-in (`AUDIT_RETENTION_ENABLED=false` by default) and uses
the default/security/payment/high-severity day values, bounded batch size, and ISO-8601 interval in
`.env.example`. Integrity verification is controlled by `AUDIT_INTEGRITY_ENABLED`, batch size, and interval.
Retention values are organization/product policy choices, not legal-retention claims. Keep the application
zone explicit in production and tests.

Configure `AUDIT_REQUEST_CONTEXT_ENABLED`, header names, `AUDIT_CAPTURE_IP`, `AUDIT_MASK_IP`,
`AUDIT_CAPTURE_USER_AGENT`, and `AUDIT_TRUST_FORWARDED_HEADERS`. Defaults enable IDs/user agent,
disable IP, mask enabled IPs, and distrust forwarding headers. Never enable forwarded-header trust
unless the application is reachable only through a configured trusted proxy. Console logs include
request and correlation MDC values; production SQL display is disabled.

## Refund-1 setup

Refund-1 requires only the V34 database migration. It adds no environment variables, scheduler,
executor, message broker, callback, or provider credential. eSewa payment credentials do not enable
refund execution. HTTP access is read-only; intent creation is an internal service boundary for
future reviewed workflows. Keep `APP_TIME_ZONE` explicit because requested timestamps and reference
dates use the injected application `Clock`.

## Refund-2 setup

## Observability-2 setup

Set `APP_ENVIRONMENT=local` (or the bounded deployment environment name). All meters include common
`application=hamro-chalchitraghar` and `environment` tags. No Prometheus server is required locally; an ADMIN
can inspect `/actuator/prometheus` for `chalchitraghar_booking_operations_total`,
`chalchitraghar_job_executions_total`, and executor metrics. Keep real ADMIN tokens out of shell history.

Refund-2 requires V35 and no provider credential or new feature flag. Customer requests use existing
`BOOKING_CANCELLATION_CUTOFF_MINUTES`. Refund email uses the existing after-commit notification queue;
in-app/audit behavior remains when email is disabled. There is no refund worker, callback, scheduler,
eSewa refund credential, or asynchronous financial processing.
Refund processing uses `REFUND_PROCESSING_ENABLED`, `REFUND_AUTO_PROCESS_ENABLED` (default false), `REFUND_DEFAULT_METHOD` (MANUAL), bounded attempt/retry delay properties, processing timeout, batch sizes, and worker ID shown in `.env.example`. Startup requires no provider refund credentials. Enabling eSewa payments does not enable eSewa refunds.
Refund-4 settings include `REFUND_REPORTS_MAX_RANGE_DAYS`, `REFUND_CONSISTENCY_BATCH_SIZE`, and disabled-by-default retention settings in `.env.example`. Retention periods are operational defaults, not claims about legal requirements. Refund operational metrics are exported through the protected Prometheus endpoint.

The corresponding validated prefixes are `app.refunds.reports`, `app.refunds.consistency`, and `app.refunds.retention`. Retention supports only `ANONYMIZE`; it performs no hard deletion.

## Logging and database diagnostics

Development/test use the readable console pattern; production uses native ECS JSON (`LOG_FORMAT=ecs`) on stdout. `LOG_LEVEL_ROOT` and `LOG_LEVEL_APP` default to `INFO`, while production SQL and bind logging are off. Docker Compose applies bounded `json-file` rotation; other platforms own collection and retention. Inspect locally with `docker compose logs -f app`.

For production database investigation, choose a deployment-appropriate PostgreSQL `log_min_duration_statement` threshold and enable `pg_stat_statements` where approved. SQL can contain personal data, so keep bind logging disabled, restrict access/retention, and prefer fingerprints. The application does not configure these PostgreSQL server settings.

## Prometheus and Grafana

Create `monitoring/secrets/prometheus_scrape_password` without a trailing newline, set a non-placeholder `GRAFANA_ADMIN_PASSWORD`, and start the base and observability Compose files together. Provisioning is automatic. Prometheus/Grafana use loopback ports 9090/3000 by default; named volumes and 15-day/5-GB retention defaults bound storage. Validate with promtool, JSON parsing, and `docker compose ... config`. The readiness script is `monitoring/check-readiness.sh`. Never publish the scrape or Grafana anonymously, commit credentials, or treat monitoring volumes as financial/audit history.
