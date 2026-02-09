ALTER TABLE purchase_order ADD COLUMN saga_id VARCHAR(36);
ALTER TABLE purchase_order ADD COLUMN saga_state VARCHAR(50) DEFAULT 'SAGA_STARTED';
ALTER TABLE purchase_order ADD COLUMN saga_started_at TIMESTAMP;
ALTER TABLE purchase_order ADD COLUMN saga_completed_at TIMESTAMP;
ALTER TABLE purchase_order ADD COLUMN compensation_reason TEXT;

-- Create unique index on saga_id
CREATE UNIQUE INDEX idx_order_saga_id ON purchase_order(saga_id) WHERE saga_id IS NOT NULL;

-- Create index for querying orders by saga state
CREATE INDEX idx_order_saga_state ON purchase_order(saga_state);