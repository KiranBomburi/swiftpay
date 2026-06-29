-- SwiftPay Transaction Gateway schema

CREATE TABLE IF NOT EXISTS accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(64) NOT NULL UNIQUE,
    balance     NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3) NOT NULL DEFAULT 'INR',
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(128) NOT NULL UNIQUE,
    sender_id       VARCHAR(64) NOT NULL,
    receiver_id     VARCHAR(64) NOT NULL,
    amount          NUMERIC(18, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL DEFAULT 'INR',
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    failure_reason  TEXT,
    idempotency_key VARCHAR(128),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE INDEX idx_payments_sender   ON payments(sender_id);
CREATE INDEX idx_payments_receiver ON payments(receiver_id);
CREATE INDEX idx_payments_status   ON payments(status);
CREATE INDEX idx_payments_created  ON payments(created_at DESC);

-- Seed some test accounts
INSERT INTO accounts (user_id, balance, currency) VALUES
    ('user_001', 50000.00, 'INR'),
    ('user_002', 25000.00, 'INR'),
    ('user_003', 10000.00, 'INR'),
    ('user_004', 75000.00, 'INR')
ON CONFLICT (user_id) DO NOTHING;
