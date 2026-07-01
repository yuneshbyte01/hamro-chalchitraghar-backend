CREATE TABLE booking_seats (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    booking_id BIGINT NOT NULL,
    seat_id BIGINT NOT NULL,
    CONSTRAINT fk_booking_seats_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_seats_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id)
);
