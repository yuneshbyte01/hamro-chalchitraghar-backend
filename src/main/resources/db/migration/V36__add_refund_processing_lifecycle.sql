ALTER TABLE refunds ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE refunds ADD COLUMN max_attempts INTEGER NOT NULL DEFAULT 3;
ALTER TABLE refunds ADD COLUMN next_attempt_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN last_attempt_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN processing_started_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN processed_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN failed_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN claimed_at TIMESTAMP;
ALTER TABLE refunds ADD COLUMN claimed_by VARCHAR(100);
ALTER TABLE refunds ADD COLUMN last_failure_code VARCHAR(100);
ALTER TABLE refunds ADD COLUMN provider_status VARCHAR(100);
ALTER TABLE refunds ADD COLUMN manual_review_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_attempt_count CHECK (attempt_count >= 0);
ALTER TABLE refunds ADD CONSTRAINT ck_refunds_max_attempts CHECK (max_attempts > 0);
CREATE INDEX idx_refunds_processing_due ON refunds(status, next_attempt_at, created_at);
CREATE INDEX idx_refunds_processing_claim ON refunds(status, claimed_at);
CREATE INDEX idx_refunds_payment_status ON refunds(payment_id, status);

CREATE TABLE refund_attempts (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
 refund_id BIGINT NOT NULL, attempt_number INTEGER NOT NULL, method VARCHAR(30) NOT NULL,
 provider VARCHAR(30), status VARCHAR(30) NOT NULL, started_at TIMESTAMP NOT NULL,
 completed_at TIMESTAMP, provider_refund_reference VARCHAR(255), provider_status VARCHAR(100),
 failure_code VARCHAR(100), failure_reason VARCHAR(500), correlation_id VARCHAR(100),
 CONSTRAINT fk_refund_attempts_refund FOREIGN KEY(refund_id) REFERENCES refunds(id),
 CONSTRAINT uk_refund_attempt_number UNIQUE(refund_id, attempt_number),
 CONSTRAINT ck_refund_attempt_number CHECK(attempt_number > 0),
 CONSTRAINT ck_refund_attempt_status CHECK(status IN ('STARTED','SUCCEEDED','FAILED','UNKNOWN'))
);
CREATE INDEX idx_refund_attempt_status_started ON refund_attempts(status, started_at);
CREATE INDEX idx_refund_attempt_provider_reference ON refund_attempts(provider_refund_reference);
