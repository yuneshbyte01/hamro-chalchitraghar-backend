ALTER TABLE payments ADD COLUMN expires_at TIMESTAMP;
ALTER TABLE payments ADD CONSTRAINT uk_payments_booking_idempotency UNIQUE (booking_id, idempotency_key);
CREATE INDEX idx_payments_booking_status ON payments (booking_id, status);
CREATE INDEX idx_payments_status_expires ON payments (status, expires_at);
CREATE INDEX idx_payments_booking_idempotency ON payments (booking_id, idempotency_key);
