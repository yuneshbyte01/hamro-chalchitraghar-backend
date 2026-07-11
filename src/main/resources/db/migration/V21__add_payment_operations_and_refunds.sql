ALTER TABLE payments ADD COLUMN failure_reason VARCHAR(50);
CREATE INDEX idx_payments_manual_review ON payments (manual_review_required);
CREATE INDEX idx_payments_verification_time ON payments (verification_time);
CREATE INDEX idx_payments_amount ON payments (amount);
CREATE TABLE refunds (
 id BIGSERIAL PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
 payment_id BIGINT NOT NULL REFERENCES payments(id), refund_reference VARCHAR(50) NOT NULL UNIQUE,
 amount NUMERIC(12,2) NOT NULL CHECK(amount > 0), status VARCHAR(30) NOT NULL,
 provider_refund_id VARCHAR(255), completed_at TIMESTAMP
);
CREATE INDEX idx_refunds_payment ON refunds(payment_id);
