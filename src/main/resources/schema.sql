CREATE TABLE IF NOT EXISTS trading_flow_history (
    id VARCHAR(36) PRIMARY KEY,
    market VARCHAR(50) NOT NULL,
    current_price NUMERIC(19, 8),
    signal_type VARCHAR(20),
    signal_reason VARCHAR(500),
    order_created BOOLEAN NOT NULL,
    order_status VARCHAR(20),
    message VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_trading_flow_history_created_at
    ON trading_flow_history (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_trading_flow_history_market_created_at
    ON trading_flow_history (market, created_at DESC);

CREATE TABLE IF NOT EXISTS telegram_update_offset (
    id VARCHAR(50) PRIMARY KEY,
    last_update_offset BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
