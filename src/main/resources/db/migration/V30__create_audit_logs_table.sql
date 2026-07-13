CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    occurred_at TIMESTAMP NOT NULL,
    actor_user_id BIGINT NULL,
    actor_email_snapshot VARCHAR(320) NULL,
    actor_role VARCHAR(50) NULL,
    actor_type VARCHAR(30) NOT NULL,
    action VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id BIGINT NULL,
    resource_reference VARCHAR(200) NULL,
    result VARCHAR(30) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    request_id VARCHAR(100) NULL,
    correlation_id VARCHAR(100) NULL,
    before_values TEXT NULL,
    after_values TEXT NULL,
    metadata TEXT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_audit_logs_occurred ON audit_logs (occurred_at DESC);
CREATE INDEX idx_audit_logs_actor ON audit_logs (actor_user_id, occurred_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs (action, occurred_at DESC);
CREATE INDEX idx_audit_logs_category ON audit_logs (category, occurred_at DESC);
CREATE INDEX idx_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX idx_audit_logs_correlation ON audit_logs (correlation_id);
CREATE INDEX idx_audit_logs_request ON audit_logs (request_id);
CREATE INDEX idx_audit_logs_result_severity ON audit_logs (result, severity, occurred_at DESC);
