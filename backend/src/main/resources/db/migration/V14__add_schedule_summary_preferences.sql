ALTER TABLE users
    ADD COLUMN daily_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN daily_summary_time TIME NOT NULL DEFAULT '08:00',
    ADD COLUMN weekly_summary_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN weekly_summary_day INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN weekly_summary_time TIME NOT NULL DEFAULT '18:00';

ALTER TABLE users ADD CONSTRAINT ck_users_weekly_summary_day CHECK (weekly_summary_day BETWEEN 1 AND 7);

CREATE TABLE summary_deliveries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    summary_type VARCHAR(10) NOT NULL,
    period_start DATE NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_summary_delivery UNIQUE (user_id, summary_type, period_start),
    CONSTRAINT ck_summary_delivery_type CHECK (summary_type IN ('DAILY', 'WEEKLY'))
);
