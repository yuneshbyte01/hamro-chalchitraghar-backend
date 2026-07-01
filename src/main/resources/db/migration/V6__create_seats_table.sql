CREATE TABLE seats (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    show_id BIGINT NOT NULL,
    seat_number INTEGER NOT NULL,
    row_label VARCHAR(255) NOT NULL,
    seat_code VARCHAR(255) NOT NULL,
    seat_type VARCHAR(255) NOT NULL,
    price DOUBLE PRECISION NOT NULL,
    seat_status VARCHAR(255) NOT NULL,
    position_index INTEGER NOT NULL,
    locked_at TIMESTAMP,
    lock_expires_at TIMESTAMP,
    CONSTRAINT fk_seats_show
        FOREIGN KEY (show_id) REFERENCES shows (id)
);
