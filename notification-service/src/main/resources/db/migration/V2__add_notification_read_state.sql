ALTER TABLE notifications ADD COLUMN read_at TIMESTAMP;

CREATE INDEX idx_notifications_user_unread ON notifications (user_id, read_at);
