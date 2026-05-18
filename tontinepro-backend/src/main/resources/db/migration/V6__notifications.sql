-- V6 – Table des notifications in-app
CREATE TABLE notifications (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type           VARCHAR(50)  NOT NULL,
    titre          VARCHAR(255) NOT NULL,
    message        TEXT         NOT NULL,
    lu             BOOLEAN      NOT NULL DEFAULT FALSE,
    reference_id   UUID,
    reference_type VARCHAR(50),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user     ON notifications(user_id);
CREATE INDEX idx_notifications_user_lu  ON notifications(user_id, lu);
CREATE INDEX idx_notifications_date     ON notifications(created_at DESC);
