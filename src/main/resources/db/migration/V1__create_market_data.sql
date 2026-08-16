CREATE TABLE market_data (
    id UUID PRIMARY KEY,
    exchange TEXT NOT NULL,
    symbol TEXT NOT NULL,
    last_price NUMERIC(30, 12) NOT NULL,
    open NUMERIC(30, 12) NOT NULL,
    high NUMERIC(30, 12) NOT NULL,
    low NUMERIC(30, 12) NOT NULL,
    volume NUMERIC(30, 12) NOT NULL,
    turnover NUMERIC(30, 12) NOT NULL,
    ts TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_market_data_symbol_ts ON market_data (symbol, ts DESC);
