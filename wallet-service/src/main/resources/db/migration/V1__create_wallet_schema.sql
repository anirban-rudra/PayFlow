CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    available_balance NUMERIC(19,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_wallets_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT chk_wallets_available_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT chk_wallets_available_lte_balance CHECK (available_balance <= balance)
);

CREATE UNIQUE INDEX uk_wallets_user_id ON wallets (user_id);
CREATE INDEX idx_wallets_user_id ON wallets (user_id);

CREATE TABLE wallet_holds (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL REFERENCES wallets(id),
    hold_reference VARCHAR(128) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    expires_at TIMESTAMP,
    CONSTRAINT chk_wallet_holds_amount_positive CHECK (amount > 0)
);

CREATE UNIQUE INDEX uk_wallet_holds_reference ON wallet_holds (hold_reference);
CREATE INDEX idx_wallet_holds_reference ON wallet_holds (hold_reference);
CREATE INDEX idx_wallet_holds_status_expires_at ON wallet_holds (status, expires_at);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT chk_wallet_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_wallet_transactions_wallet_id ON transactions (wallet_id);
