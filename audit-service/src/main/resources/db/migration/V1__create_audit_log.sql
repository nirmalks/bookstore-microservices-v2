CREATE TABLE audit_log (
    id BIGSERIAL, -- logical ID, but not the sole PK
    event_id VARCHAR(64) NOT NULL,
    schema_version VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    service_name VARCHAR(80) NOT NULL,
    environment VARCHAR(32),
    action VARCHAR(80) NOT NULL,
    resource VARCHAR(80) NOT NULL,
    resource_id VARCHAR(128),
    status VARCHAR(16) NOT NULL,
    principal VARCHAR(120),
    trace_id VARCHAR(64),
    span_id VARCHAR(64),
    idempotency_key VARCHAR(300) NOT NULL,
    detail VARCHAR(500),
    error_code VARCHAR(120),
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    -- Partitioning requires the partition key to be part of the Primary Key
    PRIMARY KEY (occurred_at, id)
) PARTITION BY RANGE (occurred_at);

-- Safety net partition to prevent write failures when a month partition is missing.
CREATE TABLE audit_log_default PARTITION OF audit_log DEFAULT;

-- Create partitions from previous month up to 12 months ahead.
DO $$
DECLARE
    month_start DATE := date_trunc('month', NOW())::date - INTERVAL '1 month';
    i INT;
    p_start DATE;
    p_end DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..13 LOOP
        p_start := (month_start + (i || ' month')::interval)::date;
        p_end := (month_start + ((i + 1) || ' month')::interval)::date;
        partition_name := format('audit_log_%s', to_char(p_start, 'YYYY_MM'));

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_log FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            p_start,
            p_end
        );
    END LOOP;
END $$;

-- Operational helper: call monthly (via job/runbook) to keep partitions pre-created.
CREATE OR REPLACE FUNCTION create_audit_log_partitions(months_ahead INT DEFAULT 12)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    month_start DATE := date_trunc('month', NOW())::date;
    i INT;
    p_start DATE;
    p_end DATE;
    partition_name TEXT;
BEGIN
    FOR i IN 0..months_ahead LOOP
        p_start := (month_start + (i || ' month')::interval)::date;
        p_end := (month_start + ((i + 1) || ' month')::interval)::date;
        partition_name := format('audit_log_%s', to_char(p_start, 'YYYY_MM'));

        EXECUTE format(
            'CREATE TABLE IF NOT EXISTS %I PARTITION OF audit_log FOR VALUES FROM (%L) TO (%L)',
            partition_name,
            p_start,
            p_end
        );
    END LOOP;
END $$;

-- Index definitions must be compliant with partitioning
CREATE UNIQUE INDEX uk_audit_idempotency ON audit_log(occurred_at, idempotency_key);
CREATE INDEX idx_audit_event_id ON audit_log(event_id);
CREATE INDEX idx_audit_principal ON audit_log(principal);
CREATE INDEX idx_audit_action ON audit_log(action);
CREATE INDEX idx_audit_resource ON audit_log(resource);
CREATE INDEX idx_audit_trace ON audit_log(trace_id);
