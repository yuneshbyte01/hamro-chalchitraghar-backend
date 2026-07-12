ALTER TABLE tickets
    ADD COLUMN reissued_at TIMESTAMP NULL,
    ADD COLUMN reissued_by_user_id BIGINT NULL,
    ADD COLUMN reissue_reason VARCHAR(500) NULL,
    ADD CONSTRAINT fk_tickets_reissued_by FOREIGN KEY (reissued_by_user_id) REFERENCES users(id);
CREATE INDEX idx_tickets_status_issued ON tickets(status, issued_at);
CREATE INDEX idx_tickets_status_expired ON tickets(status, expired_at);
CREATE INDEX idx_tickets_checked_in_at ON tickets(checked_in_at);
CREATE INDEX idx_ticket_validations_result_time ON ticket_validations(result, validation_time);

CREATE TABLE ticket_deliveries (
    id BIGSERIAL PRIMARY KEY, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP NOT NULL,
    booking_id BIGINT NOT NULL, channel VARCHAR(30) NOT NULL, recipient VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL, attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP NULL, sent_at TIMESTAMP NULL, failure_message VARCHAR(500) NULL,
    CONSTRAINT fk_ticket_deliveries_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT uk_ticket_deliveries_booking_channel UNIQUE (booking_id, channel)
);
CREATE INDEX idx_ticket_deliveries_status_attempt ON ticket_deliveries(status, attempt_count);
CREATE INDEX idx_ticket_deliveries_booking ON ticket_deliveries(booking_id);
