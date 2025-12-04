CREATE TYPE event_status_enum AS ENUM('PENDING', 'SENT', 'FAILED');

CREATE TABLE outbox (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status event_status_enum NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_outbox_status ON outbox (status);