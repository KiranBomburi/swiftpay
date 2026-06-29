-- SwiftPay Analytics Worker schema (mock OLAP table)

CREATE TABLE IF NOT EXISTS payment_analytics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(128) NOT NULL UNIQUE,
    sender_id       VARCHAR(64) NOT NULL,
    receiver_id     VARCHAR(64) NOT NULL,
    amount          NUMERIC(18, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    processed_at    TIMESTAMP NOT NULL,
    ingested_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Aggregation-friendly indexes
CREATE INDEX idx_analytics_processed_at ON payment_analytics(processed_at DESC);
CREATE INDEX idx_analytics_currency     ON payment_analytics(currency);
CREATE INDEX idx_analytics_status       ON payment_analytics(status);
