ALTER TABLE transactions ADD COLUMN public_reference VARCHAR(32);
ALTER TABLE transactions ADD COLUMN hold_reference VARCHAR(128);
ALTER TABLE transactions ADD COLUMN failure_reason TEXT;
ALTER TABLE transactions ADD COLUMN updated_at TIMESTAMP;
ALTER TABLE transactions ADD COLUMN completed_at TIMESTAMP;

UPDATE transactions
SET public_reference = 'PF-TXN-' || LPAD(id::TEXT, 10, '0'),
    updated_at = COALESCE(timestamp, CURRENT_TIMESTAMP),
    completed_at = CASE WHEN status = 'SUCCESS' THEN timestamp ELSE NULL END
WHERE public_reference IS NULL;

ALTER TABLE transactions ALTER COLUMN public_reference SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN updated_at SET NOT NULL;

CREATE UNIQUE INDEX uk_transactions_public_reference ON transactions (public_reference);
CREATE INDEX idx_transactions_status_updated_at ON transactions (status, updated_at);
CREATE INDEX idx_transactions_hold_reference ON transactions (hold_reference);
