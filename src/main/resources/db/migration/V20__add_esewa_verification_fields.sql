ALTER TABLE payments ADD COLUMN provider_reference VARCHAR(255);
ALTER TABLE payments ADD COLUMN verification_time TIMESTAMP;
ALTER TABLE payments ADD COLUMN manual_review_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE payments ADD COLUMN manual_review_reason VARCHAR(500);
ALTER TABLE payments ADD COLUMN provider_status VARCHAR(50);
CREATE UNIQUE INDEX uk_payments_provider_transaction ON payments (provider, provider_transaction_id) WHERE provider_transaction_id IS NOT NULL;
CREATE INDEX idx_payments_provider_reference ON payments (provider_reference);
