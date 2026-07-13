CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    event_key VARCHAR(200) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    payload TEXT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_notifications_user_event_channel UNIQUE (user_id, event_key, channel),
    CONSTRAINT ck_notifications_title_not_blank CHECK (BTRIM(title) <> ''),
    CONSTRAINT ck_notifications_message_not_blank CHECK (BTRIM(message) <> ''),
    CONSTRAINT ck_notifications_event_key_not_blank CHECK (BTRIM(event_key) <> '')
);

CREATE INDEX idx_notifications_user_occurred
    ON notifications(user_id, occurred_at DESC);
CREATE INDEX idx_notifications_user_read_occurred
    ON notifications(user_id, is_read, occurred_at DESC);
CREATE INDEX idx_notifications_type_occurred
    ON notifications(type, occurred_at DESC);
CREATE INDEX idx_notifications_channel_occurred
    ON notifications(channel, occurred_at DESC);
