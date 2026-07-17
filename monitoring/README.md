# Monitoring operations

This optional stack deploys Prometheus and Grafana beside the application. Prometheus stores bounded operational time series; Grafana only visualizes them. Neither is authoritative for bookings, payments, refunds, revenue, audit history, or delivery state.

## Start securely

Create the ignored scrape-secret file and choose a Grafana password:

```bash
mkdir -p monitoring/secrets
printf '%s' 'replace-with-a-random-local-secret' > monitoring/secrets/prometheus_scrape_password
export GRAFANA_ADMIN_PASSWORD='replace-with-a-different-password'
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Local endpoints bind to loopback by default:

- application: `http://localhost:8080`
- Prometheus: `http://localhost:9090`
- Grafana: `http://localhost:3000`

The app and Prometheus receive the same Docker secret through different mechanisms. Prometheus uses HTTP Basic authentication with the fixed, non-personal `prometheus` username. Admin JWT access remains supported, while public/customer/staff access remains denied. In production, keep app management, Prometheus, and Grafana on protected networks; inject secrets through the platform, use TLS at the ingress, disable anonymous Grafana, and authorize dashboard access by operator role.

`PROMETHEUS_RETENTION_TIME` defaults to 15 days and `PROMETHEUS_RETENTION_SIZE` to 5 GB. Both limits apply. Monitoring data volumes survive recreation. `docker compose -f docker-compose.yml -f docker-compose.observability.yml down -v` permanently deletes PostgreSQL and monitoring volumes—review the composed project before using it.

## Provisioned dashboards

- Application Overview: availability, HTTP errors, heap, Hikari, and executor rejections.
- JVM and Infrastructure: CPU, GC, threads, connection-pool and executor capacity.
- Business Operations: bounded booking, ticket, notification, and email outcome rates.
- Jobs and Workers: job outcomes/freshness/throughput and executor pressure.
- Payments and Refunds: operational outcomes/provider latency and provisional availability SLI.

Counters reset with the process. Dashboards intentionally contain no user or business identifiers, request/correlation IDs, QR values, or provider references. Financial totals belong in database-backed admin reports.

## Initial SLIs and SLOs

These are **initial operational targets subject to production baseline validation**, not contractual guarantees:

| SLI | Initial objective | Measurement boundary |
|---|---:|---|
| Application availability | 99.5% monthly | eligible non-5xx HTTP responses / eligible requests; exclude health and metrics when refining recording rules |
| Request latency | 95% under 750 ms | defer percentile enforcement until HTTP histograms are deliberately enabled and validated |
| Payment reliability | 98% successful eligible payment operations | bounded payment outcomes; manual review is not success |
| Notification delivery | 97% sent eligible attempts | delivery attempts only, not notification creation |
| Critical job freshness | success within 3× expected interval | per-job schedules; restart resets the in-memory last-success gauge |

The availability error budget is `1 - 0.995 = 0.5%`. Meaningful burn-rate alerting requires enough production traffic and history, so the initial rules use sustained ratios and volume gates. Tune thresholds after observing real traffic; local testing does not validate an SLO.

Expected job intervals currently include booking/seat/ticket expiry at roughly one minute, payment reconciliation at five minutes, retry jobs at one minute, show reconciliation at one minute, reminders at five minutes, and retention/integrity work at roughly one day when enabled. The generic 30-minute freshness alert covers frequent critical jobs only and must be split into schedule-specific rules before enabling materially different production schedules.

## Capacity planning

Review trends rather than single peaks: heap headroom and GC, process CPU, live threads, Hikari active/max and pending connections, notification executor active/queue/completion/rejections, job duration versus configured interval, and booking/payment/notification/refund throughput. A longer job duration than its interval, sustained pending DB connections, or rising executor queue with weak completion rate is a capacity signal—not a reason to automatically fail readiness.

## Synthetic readiness

`monitoring/check-readiness.sh` performs a non-mutating readiness GET with a five-second default timeout. Run it from the deployment network or an external uptime system. It needs no secret because readiness intentionally exposes status only. Never synthetic-test real payment or refund operations.

## Alert routing and severity

- **Critical:** application/database unavailable, sustained payment/refund outage, audit integrity or unrecoverable consistency risk. Page the owning operator after routing is configured.
- **Warning:** sustained errors, stale jobs, queue/pool pressure, delivery failures, high resource use. Route to the operations channel/ticket queue unless escalation criteria are met.
- **Informational:** deployment/restart and isolated transient failures; dashboards/logs, not paging.

No webhook or pager credential is committed. Configure Alertmanager or the platform alert router later with named ownership, inhibition/grouping, maintenance windows, and secret-managed receivers.

## Application down

Check Prometheus target status, readiness/liveness, container state and structured startup/shutdown logs, then PostgreSQL health. Inspect JVM exit/restart evidence. Restart only after determining whether it would hide a database/configuration failure; escalate if readiness remains down or persisted work cannot recover.

## Database unavailable or pool saturated

Use JVM/Infrastructure panels for Hikari active/max/pending, check PostgreSQL health and connection limits, and inspect approved slow-query fingerprints. Do not enable bind logging or terminate unknown queries blindly. Escalate sustained readiness failure or connection exhaustion.

## High 5xx rate

Break down templated URI/status metrics, obtain correlation IDs from responses, inspect the single unexpected-exception log, and compare recent deployments/config changes. Use audit and database state for business truth. Roll back a verified bad deployment; do not suppress the alert by widening thresholds during an active incident.

## Payment provider failure

Inspect payment outcomes/provider duration, bounded provider failure logs, reconciliation and manual-review queues. Verify provider status externally. Never replay callbacks, mark success, or retry unknown outcomes without independent evidence.

## Refund processing failure

Inspect append-only attempts, manual-review/reconciliation state, stale claims and provider support evidence. Do not mark a refund successful without verified completion and a controlled reference. Escalate persistent provider or consistency uncertainty.

## Notification delivery outage

Inspect executor queue/active/completion/rejections, SMTP failure category, persisted delivery status and retry count. Restore capacity/provider access and let bounded retries recover. Do not trigger an unbounded mass resend or use email bodies from logs.

## Scheduled job stale or failing

Check last-success telemetry, the fixed job name and job-run correlation log, scheduler enablement, duration versus interval, row locks/claims, and persisted retryable rows. Remember restart resets freshness. Restart only when safe and avoid concurrent manual invocation that can duplicate work.

Reporting adds `scheduled_report_dispatch` and `scheduled_report_retry` job telemetry plus bounded `report_delivery_total` and `report_delivery_failures_total` counters tagged only by status/format. Alert on repeated failures or stale dispatcher success; never tag recipient, schedule ID, filename, or error text.

Reporting HTTP telemetry includes request counts, duration, and query failures tagged by stable route template and status. Export/generation/delivery metrics remain bounded by report type, format, and status. Date ranges, recipients, filenames, entity titles, and identifiers must never become metric labels. Establish duration or failure alerts only after observing a representative baseline.

## Audit integrity failure

Preserve database and log evidence, restrict access, compare append-only audit history with correlated operational events, and escalate immediately to the system owner. Do not repair/delete audit rows or rotate evidence during investigation.

## Validation and upgrades

Run `promtool check config monitoring/prometheus/prometheus.yml`, `promtool check rules monitoring/prometheus/rules/*.yml`, parse every dashboard JSON, and run `docker compose ... config` before deployment. Pin image updates, read release notes, back up dashboards/data as appropriate, validate provisioning in a non-production environment, and keep metric names/labels stable. New alerts require a sustained condition, severity, runbook, dashboard, known metric, and bounded labels.
