CREATE TABLE saga_audit_log (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(36) NOT NULL,
    order_id VARCHAR(36),
    previous_state VARCHAR(50),
    new_state VARCHAR(50),
    event_type VARCHAR(100),
    source_service VARCHAR(100),
    message TEXT,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_saga_audit_saga_id ON saga_audit_log(saga_id);

CREATE INDEX idx_saga_audit_timestamp ON saga_audit_log(timestamp);
