CREATE TABLE reminder_deliveries (
    id BIGSERIAL PRIMARY KEY,
    schedule_id BIGINT NOT NULL REFERENCES schedules(id) ON DELETE CASCADE,
    reminder_minutes INTEGER NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reminder_delivery UNIQUE (schedule_id, reminder_minutes)
);
