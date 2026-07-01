CREATE TABLE halls (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    name VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    layout_ref VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    CONSTRAINT uk_halls_name UNIQUE (name)
);

CREATE INDEX idx_hall_name ON halls (name);
CREATE INDEX idx_hall_status ON halls (status);
