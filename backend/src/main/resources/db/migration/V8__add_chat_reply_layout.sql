ALTER TABLE users
    ADD COLUMN chat_reply_layout VARCHAR(20) NOT NULL DEFAULT 'BALANCED';

ALTER TABLE users
    ADD CONSTRAINT ck_users_chat_reply_layout
        CHECK (chat_reply_layout IN ('COMPACT', 'BALANCED', 'SECTIONED'));
