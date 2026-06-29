-- SwiftPay Ledger Service schema

CREATE TABLE IF NOT EXISTS ledger_accounts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR(64) NOT NULL UNIQUE,
    balance     NUMERIC(18, 2) NOT NULL DEFAULT 0.00,
    currency    VARCHAR(3) NOT NULL DEFAULT 'INR',
    version     BIGINT NOT NULL DEFAULT 0,        -- optimistic locking
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ledger_entries (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  VARCHAR(128) NOT NULL,
    user_id         VARCHAR(64) NOT NULL,
    entry_type      VARCHAR(10) NOT NULL,          -- DEBIT or CREDIT
    amount          NUMERIC(18, 2) NOT NULL,
    balance_after   NUMERIC(18, 2) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    status          VARCHAR(20) NOT NULL,
    failure_reason  TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_entry_type CHECK (entry_type IN ('DEBIT', 'CREDIT'))
);

CREATE INDEX idx_ledger_entries_txn    ON ledger_entries(transaction_id);
CREATE INDEX idx_ledger_entries_user   ON ledger_entries(user_id);
CREATE INDEX idx_ledger_entries_created ON ledger_entries(created_at DESC);

-- Mirror seed accounts from gateway
INSERT INTO ledger_accounts (user_id, balance, currency) VALUES
    ('user_001', 50000.00, 'INR'),
    ('user_002', 25000.00, 'INR'),
    ('user_003', 10000.00, 'INR'),
    ('user_004', 75000.00, 'INR')
ON CONFLICT (user_id) DO NOTHING;
