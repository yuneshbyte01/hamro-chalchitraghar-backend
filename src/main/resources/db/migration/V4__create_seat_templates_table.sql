CREATE TABLE seat_templates (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    hall_id BIGINT NOT NULL,
    row_label VARCHAR(255) NOT NULL,
    seat_number INTEGER NOT NULL,
    seat_code VARCHAR(255) NOT NULL,
    seat_type VARCHAR(255) NOT NULL,
    position_index INTEGER NOT NULL,
    CONSTRAINT fk_seat_templates_hall
        FOREIGN KEY (hall_id) REFERENCES halls (id)
);
