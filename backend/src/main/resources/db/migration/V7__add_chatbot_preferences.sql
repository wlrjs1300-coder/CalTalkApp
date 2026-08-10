ALTER TABLE users
    ADD COLUMN chat_reply_style VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN chat_reply_density VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN chat_time_format VARCHAR(20) NOT NULL DEFAULT 'TWELVE_HOUR',
    ADD COLUMN chat_confirm_create BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN chat_confirm_update BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN chat_default_duration_minutes INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN chat_default_query_range VARCHAR(20) NOT NULL DEFAULT 'TODAY';

ALTER TABLE users
    ADD CONSTRAINT ck_users_chat_reply_style
        CHECK (chat_reply_style IN ('CONCISE', 'STANDARD', 'ASSISTANT', 'BUSINESS', 'FRIENDLY')),
    ADD CONSTRAINT ck_users_chat_reply_density
        CHECK (chat_reply_density IN ('ESSENTIAL', 'STANDARD', 'DETAILED')),
    ADD CONSTRAINT ck_users_chat_time_format
        CHECK (chat_time_format IN ('TWELVE_HOUR', 'TWENTY_FOUR_HOUR')),
    ADD CONSTRAINT ck_users_chat_duration
        CHECK (chat_default_duration_minutes IN (30, 60, 120)),
    ADD CONSTRAINT ck_users_chat_query_range
        CHECK (chat_default_query_range IN ('TODAY', 'THREE_DAYS', 'THIS_WEEK', 'NEXT_FIVE'));
