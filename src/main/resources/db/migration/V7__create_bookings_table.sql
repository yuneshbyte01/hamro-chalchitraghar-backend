CREATE TABLE bookings (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    show_id BIGINT NOT NULL,
    booking_time TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_show
        FOREIGN KEY (show_id) REFERENCES shows (id)
);
