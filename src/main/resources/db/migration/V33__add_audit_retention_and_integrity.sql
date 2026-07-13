ALTER TABLE audit_logs ADD COLUMN retention_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE audit_logs ADD COLUMN anonymized_at TIMESTAMP NULL;
ALTER TABLE audit_logs ADD COLUMN integrity_hash VARCHAR(64) NULL;

CREATE INDEX idx_audit_logs_retention ON audit_logs (retention_status, occurred_at, id);
CREATE INDEX idx_audit_logs_category_severity ON audit_logs (category, severity, occurred_at DESC);
CREATE INDEX idx_audit_logs_actor_email ON audit_logs (actor_email_snapshot, occurred_at DESC);
CREATE INDEX idx_audit_logs_resource_reference ON audit_logs (resource_reference, occurred_at DESC);

-- Existing rows predate canonical hashing; new application writes always populate this field.
