CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL,
    disabled_at TIMESTAMP NULL,
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_notification_preferences_user_type_channel UNIQUE (user_id, notification_type, channel),
    CONSTRAINT ck_notification_preferences_channel CHECK (channel = 'EMAIL')
);
CREATE INDEX idx_notification_preferences_user_channel ON notification_preferences(user_id, channel);
CREATE INDEX idx_notification_preferences_type_channel_enabled ON notification_preferences(notification_type, channel, enabled);
ALTER TABLE notifications ADD COLUMN anonymized_at TIMESTAMP NULL;
ALTER TABLE notification_deliveries ADD COLUMN anonymized_at TIMESTAMP NULL;
CREATE INDEX idx_notifications_retention ON notifications(is_read, created_at, anonymized_at);
CREATE INDEX idx_notification_deliveries_retention ON notification_deliveries(status, created_at, anonymized_at);
CREATE INDEX idx_bookings_reminder_scan ON bookings(status, show_id, id);
