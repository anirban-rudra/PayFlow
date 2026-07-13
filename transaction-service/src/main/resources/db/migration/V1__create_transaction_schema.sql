CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128),
    CONSTRAINT chk_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transactions_different_parties CHECK (sender_id <> receiver_id)
);

CREATE INDEX idx_transactions_sender_id ON transactions (sender_id);
CREATE INDEX idx_transactions_receiver_id ON transactions (receiver_id);
CREATE UNIQUE INDEX uk_transactions_sender_idempotency
    ON transactions (sender_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE transaction_outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id BIGINT NOT NULL,
    event_key VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    next_attempt_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    last_error TEXT
);

CREATE INDEX idx_outbox_status_next_attempt ON transaction_outbox_events (status, next_attempt_at);
CREATE INDEX idx_outbox_aggregate_id ON transaction_outbox_events (aggregate_id);
