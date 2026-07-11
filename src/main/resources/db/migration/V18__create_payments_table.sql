CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    booking_id BIGINT NOT NULL,
    payment_reference VARCHAR(50) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    method VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider_transaction_id VARCHAR(255),
    idempotency_key VARCHAR(255),
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    initiated_at TIMESTAMP,
    completed_at TIMESTAMP,
    failed_at TIMESTAMP,
    expired_at TIMESTAMP,
    cancelled_at TIMESTAMP,
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT uk_payments_reference UNIQUE (payment_reference),
    CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_payments_currency_length CHECK (CHAR_LENGTH(currency) = 3)
);
CREATE INDEX idx_payments_booking ON payments (booking_id);
CREATE INDEX idx_payments_status ON payments (status);
CREATE INDEX idx_payments_provider_status ON payments (provider, status);
CREATE INDEX idx_payments_created_at ON payments (created_at);
CREATE INDEX idx_payments_provider_transaction_id ON payments (provider_transaction_id);
