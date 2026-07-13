ALTER TABLE audit_logs ADD COLUMN event_id VARCHAR(200) NULL;
CREATE UNIQUE INDEX uq_audit_logs_event_id ON audit_logs (event_id);
