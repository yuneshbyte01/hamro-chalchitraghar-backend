ALTER TABLE tickets
    ADD COLUMN qr_token_encrypted TEXT NOT NULL,
    ADD COLUMN qr_token_hash VARCHAR(128) NOT NULL,
    ADD COLUMN qr_issued_at TIMESTAMP NOT NULL;

ALTER TABLE tickets
    ALTER COLUMN qr_token_version SET NOT NULL,
    ALTER COLUMN qr_key_id SET NOT NULL;

ALTER TABLE tickets ADD CONSTRAINT uk_tickets_qr_token_hash UNIQUE (qr_token_hash);
CREATE INDEX idx_tickets_qr_token_hash ON tickets(qr_token_hash);
