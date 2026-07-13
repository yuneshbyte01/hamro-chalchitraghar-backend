ALTER TABLE audit_logs ADD COLUMN ip_address VARCHAR(100) NULL;
ALTER TABLE audit_logs ADD COLUMN user_agent VARCHAR(512) NULL;
ALTER TABLE audit_logs ADD COLUMN http_method VARCHAR(16) NULL;
ALTER TABLE audit_logs ADD COLUMN request_path VARCHAR(500) NULL;

CREATE INDEX idx_audit_logs_request_path ON audit_logs (request_path, occurred_at DESC);
