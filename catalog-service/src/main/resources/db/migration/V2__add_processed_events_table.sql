-- Table to track processed events for consumer idempotency

CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_processed_events_processed_at ON processed_events(processed_at);

COMMENT ON TABLE processed_events IS 'Tracks processed event IDs to ensure idempotent message consumption';
COMMENT ON COLUMN processed_events.event_id IS 'Unique identifier of the processed event (from OutboxMessage)';
COMMENT ON COLUMN processed_events.event_type IS 'Type of the event (e.g., ORDER_CREATED)';
COMMENT ON COLUMN processed_events.processed_at IS 'Timestamp when the event was processed';
