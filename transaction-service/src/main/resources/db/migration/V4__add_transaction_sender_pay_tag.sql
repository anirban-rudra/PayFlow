ALTER TABLE transactions ADD COLUMN sender_pay_tag VARCHAR(40);

CREATE INDEX idx_transactions_sender_pay_tag ON transactions (sender_pay_tag);
