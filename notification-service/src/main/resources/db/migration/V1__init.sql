-- Notification Service - V1 Initial Schema
CREATE SCHEMA IF NOT EXISTS notification_schema;

CREATE TABLE IF NOT EXISTS notification_schema.notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_id        UUID NOT NULL,
    title           VARCHAR(200) NOT NULL,
    message         TEXT NOT NULL,
    type            VARCHAR(20) NOT NULL,
    is_sent         BOOLEAN NOT NULL DEFAULT FALSE,
    error_message   VARCHAR(500),
    event_type      VARCHAR(50),
    reference_id    VARCHAR(36),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    version         BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_notif_owner_id   ON notification_schema.notifications(owner_id);
CREATE INDEX IF NOT EXISTS idx_notif_created_at ON notification_schema.notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_type       ON notification_schema.notifications(type);
