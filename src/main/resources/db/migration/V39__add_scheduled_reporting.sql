CREATE TABLE scheduled_reports (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(120) NOT NULL,
    report_type VARCHAR(40) NOT NULL,
    schedule_frequency VARCHAR(20) NOT NULL,
    delivery_format VARCHAR(10) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    currency VARCHAR(3),
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    last_run_at TIMESTAMP,
    next_run_at TIMESTAMP,
    created_by_user_id BIGINT
);

CREATE INDEX idx_scheduled_reports_due ON scheduled_reports(enabled, deleted, next_run_at);

CREATE TABLE report_deliveries (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    scheduled_report_id BIGINT NOT NULL REFERENCES scheduled_reports(id),
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    format VARCHAR(10) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP,
    next_attempt_at TIMESTAMP,
    sent_at TIMESTAMP,
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(64) NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    file_size_bytes BIGINT,
    CONSTRAINT uk_report_deliveries_idempotency UNIQUE (idempotency_key)
);

CREATE INDEX idx_report_deliveries_retry ON report_deliveries(status, next_attempt_at, attempt_count);
CREATE INDEX idx_report_deliveries_schedule_period ON report_deliveries(scheduled_report_id, period_start, period_end);
