-- create stock_reservation table for saga compensation tracking
CREATE TABLE stock_reservation (
    id BIGSERIAL PRIMARY KEY,
    saga_id VARCHAR(36) NOT NULL UNIQUE,
    order_id VARCHAR(36) NOT NULL,
    reserved_items JSONB NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMP
);

-- Index for saga lookups
CREATE INDEX idx_stock_reservation_saga_id ON stock_reservation(saga_id);

-- Index for order lookups
CREATE INDEX idx_stock_reservation_order_id ON stock_reservation(order_id);

-- Index for status-based queries (e.g., finding active reservations)
CREATE INDEX idx_stock_reservation_status ON stock_reservation(status);
