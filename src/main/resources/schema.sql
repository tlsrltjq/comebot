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

ALTER TABLE trading_flow_history
    ADD COLUMN IF NOT EXISTS exchange VARCHAR(20) NOT NULL DEFAULT 'UPBIT';

CREATE INDEX IF NOT EXISTS idx_trading_flow_history_exchange_created_at
    ON trading_flow_history (exchange, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_trading_flow_history_exchange_market_created_at
    ON trading_flow_history (exchange, market, created_at DESC);

CREATE TABLE IF NOT EXISTS paper_portfolio_state (
    exchange VARCHAR(20) PRIMARY KEY,
    cash NUMERIC(38, 18) NOT NULL DEFAULT 0,
    realized_profit NUMERIC(38, 18) NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS paper_position (
    exchange VARCHAR(20) NOT NULL,
    market VARCHAR(50) NOT NULL,
    quantity NUMERIC(38, 18) NOT NULL DEFAULT 0,
    average_buy_price NUMERIC(38, 18) NOT NULL DEFAULT 0,
    PRIMARY KEY (exchange, market)
);

CREATE INDEX IF NOT EXISTS idx_paper_position_exchange_market
    ON paper_position (exchange, market);

CREATE TABLE IF NOT EXISTS paper_realized_profit_event (
    id VARCHAR(36) PRIMARY KEY,
    exchange VARCHAR(20) NOT NULL,
    profit NUMERIC(38, 18) NOT NULL,
    realized_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_paper_realized_profit_event_exchange_realized_at
    ON paper_realized_profit_event (exchange, realized_at DESC);

CREATE TABLE IF NOT EXISTS paper_trade_log (
    id VARCHAR(36) PRIMARY KEY,
    exchange VARCHAR(20) NOT NULL,
    market VARCHAR(50) NOT NULL,
    side VARCHAR(20) NOT NULL,
    quantity NUMERIC(38, 18) NOT NULL,
    price NUMERIC(38, 18) NOT NULL,
    gross_amount NUMERIC(38, 18) NOT NULL,
    realized_profit NUMERIC(38, 18),
    executed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_paper_trade_log_exchange_executed_at
    ON paper_trade_log (exchange, executed_at DESC);

CREATE INDEX IF NOT EXISTS idx_paper_trade_log_exchange_market_executed_at
    ON paper_trade_log (exchange, market, executed_at DESC);

CREATE TABLE IF NOT EXISTS candidate_scan_log (
    id VARCHAR(36) PRIMARY KEY,
    exchange VARCHAR(20) NOT NULL,
    market VARCHAR(50) NOT NULL,
    decision VARCHAR(20) NOT NULL,
    reason VARCHAR(500),
    current_price NUMERIC(19, 8),
    price_change_rate NUMERIC(19, 4),
    high_low_range_rate NUMERIC(19, 4),
    trade_amount_change_rate NUMERIC(19, 4),
    trend VARCHAR(20),
    last_candle_bullish BOOLEAN,
    scanned_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_candidate_scan_log_exchange_scanned_at
    ON candidate_scan_log (exchange, scanned_at DESC);

CREATE INDEX IF NOT EXISTS idx_candidate_scan_log_exchange_decision_scanned_at
    ON candidate_scan_log (exchange, decision, scanned_at DESC);

CREATE TABLE IF NOT EXISTS scheduler_control_setting (
    id VARCHAR(50) PRIMARY KEY,
    auto_trading_enabled BOOLEAN NOT NULL,
    candidate_fixed_delay_ms BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS telegram_update_offset (
    id VARCHAR(50) PRIMARY KEY,
    last_update_offset BIGINT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
