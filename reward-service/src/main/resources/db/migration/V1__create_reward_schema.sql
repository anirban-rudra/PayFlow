CREATE TABLE reward (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    points NUMERIC(19,4) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    transaction_id BIGINT NOT NULL,
    CONSTRAINT chk_reward_points_non_negative CHECK (points >= 0)
);

CREATE INDEX idx_reward_user_id ON reward (user_id);
CREATE UNIQUE INDEX uk_reward_transaction_id ON reward (transaction_id);
