ALTER TABLE transactions ADD COLUMN receiver_pay_tag VARCHAR(40);

CREATE INDEX idx_transactions_receiver_pay_tag ON transactions (receiver_pay_tag);
