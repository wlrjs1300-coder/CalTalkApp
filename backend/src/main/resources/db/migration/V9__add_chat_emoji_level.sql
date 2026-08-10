ALTER TABLE users
    ADD COLUMN chat_emoji_level VARCHAR(20) NOT NULL DEFAULT 'MINIMAL';

ALTER TABLE users
    ADD CONSTRAINT chk_users_chat_emoji_level
        CHECK (chat_emoji_level IN ('NONE', 'MINIMAL', 'BALANCED'));
