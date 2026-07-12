CREATE TABLE tickets (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    booking_id BIGINT NOT NULL,
    booking_seat_id BIGINT NOT NULL,
    ticket_reference VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    checked_in_at TIMESTAMP NULL,
    revoked_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,
    checked_in_by_user_id BIGINT NULL,
    revoked_by_user_id BIGINT NULL,
    revocation_reason VARCHAR(500) NULL,
    qr_token_version INTEGER NULL,
    qr_key_id VARCHAR(100) NULL,
    CONSTRAINT fk_tickets_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_tickets_booking_seat FOREIGN KEY (booking_seat_id) REFERENCES booking_seats(id),
    CONSTRAINT fk_tickets_checked_in_by FOREIGN KEY (checked_in_by_user_id) REFERENCES users(id),
    CONSTRAINT fk_tickets_revoked_by FOREIGN KEY (revoked_by_user_id) REFERENCES users(id),
    CONSTRAINT uk_tickets_reference UNIQUE (ticket_reference),
    CONSTRAINT uk_tickets_booking_seat UNIQUE (booking_seat_id)
);

CREATE INDEX idx_tickets_booking ON tickets(booking_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_reference ON tickets(ticket_reference);
CREATE INDEX idx_tickets_booking_seat ON tickets(booking_seat_id);
CREATE INDEX idx_tickets_issued_at ON tickets(issued_at);
