ALTER TABLE refunds ADD COLUMN retention_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE refunds ADD COLUMN anonymized_at TIMESTAMP;
ALTER TABLE refund_attempts ADD COLUMN anonymized_at TIMESTAMP;
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_retention_status CHECK (retention_status IN ('ACTIVE','ANONYMIZED'));
CREATE INDEX idx_refunds_retention_requested ON refunds(retention_status, requested_at, id);
CREATE INDEX idx_refunds_processed_at ON refunds(processed_at);
CREATE INDEX idx_refunds_method_provider_status ON refunds(refund_method, provider, status);
CREATE INDEX idx_refunds_booking_status ON refunds(booking_id, status);
CREATE INDEX idx_refund_attempt_refund_status_started ON refund_attempts(refund_id, status, started_at);
