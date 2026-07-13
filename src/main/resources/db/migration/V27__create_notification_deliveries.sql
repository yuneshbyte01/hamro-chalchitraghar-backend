CREATE TABLE notification_deliveries (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    notification_id BIGINT NOT NULL,
    channel VARCHAR(30) NOT NULL,
    recipient VARCHAR(320) NULL,
    status VARCHAR(30) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL,
    next_attempt_at TIMESTAMP NULL,
    last_attempt_at TIMESTAMP NULL,
    sent_at TIMESTAMP NULL,
    failed_at TIMESTAMP NULL,
    failure_reason VARCHAR(500) NULL,
    claimed_at TIMESTAMP NULL,
    claimed_by VARCHAR(100) NULL,
    template_name VARCHAR(100) NOT NULL,
    subject VARCHAR(300) NOT NULL,
    content_version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications(id),
    CONSTRAINT uk_notification_deliveries_notification_channel
        UNIQUE (notification_id, channel),
    CONSTRAINT ck_notification_deliveries_attempt_count CHECK (attempt_count >= 0),
    CONSTRAINT ck_notification_deliveries_max_attempts CHECK (max_attempts > 0),
    CONSTRAINT ck_notification_deliveries_channel CHECK (channel = 'EMAIL')
);

CREATE INDEX idx_notification_deliveries_retry
    ON notification_deliveries(status, next_attempt_at, created_at);
CREATE INDEX idx_notification_deliveries_notification
    ON notification_deliveries(notification_id);
CREATE INDEX idx_notification_deliveries_recipient
    ON notification_deliveries(recipient, created_at);
CREATE INDEX idx_notification_deliveries_claimed
    ON notification_deliveries(claimed_at);
