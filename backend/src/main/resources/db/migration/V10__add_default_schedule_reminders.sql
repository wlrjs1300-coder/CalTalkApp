ALTER TABLE users
    ADD COLUMN default_reminder_minutes VARCHAR(100) NOT NULL DEFAULT '1440';
