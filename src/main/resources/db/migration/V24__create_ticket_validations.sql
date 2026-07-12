CREATE TABLE ticket_validations (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    ticket_id BIGINT NULL,
    validated_by_user_id BIGINT NOT NULL,
    validation_time TIMESTAMP NOT NULL,
    result VARCHAR(40) NOT NULL,
    reason VARCHAR(500),
    device_id VARCHAR(200),
    location VARCHAR(200),
    request_id VARCHAR(200),
    CONSTRAINT fk_ticket_validations_ticket FOREIGN KEY (ticket_id) REFERENCES tickets(id),
    CONSTRAINT fk_ticket_validations_user FOREIGN KEY (validated_by_user_id) REFERENCES users(id)
);
CREATE INDEX idx_ticket_validations_ticket ON ticket_validations(ticket_id);
CREATE INDEX idx_ticket_validations_time ON ticket_validations(validation_time);
CREATE INDEX idx_ticket_validations_result ON ticket_validations(result);
