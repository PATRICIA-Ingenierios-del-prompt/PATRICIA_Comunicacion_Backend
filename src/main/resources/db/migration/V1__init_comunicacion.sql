-- ================================================================
-- V1__init_comunicacion.sql
-- ================================================================

CREATE TABLE messages (
    id              VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid(),
    parche_id       VARCHAR(36)  NOT NULL,
    sender_id       VARCHAR(36)  NOT NULL,
    sender_username VARCHAR(100) NOT NULL,
    content         TEXT         NOT NULL,
    type            VARCHAR(20)  NOT NULL CHECK (type IN ('TEXT','IMAGE','FILE','SYSTEM')),
    sent_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_messages_parche_sent ON messages (parche_id, sent_at DESC);
CREATE INDEX idx_messages_sender      ON messages (sender_id);

CREATE TABLE message_read_by (
    message_id VARCHAR(36) NOT NULL REFERENCES messages(id) ON DELETE CASCADE,
    user_id    VARCHAR(36) NOT NULL,
    PRIMARY KEY (message_id, user_id)
);

-- RF-PAR-05: Solo parches privados tienen canal de voz
CREATE TABLE voice_sessions (
    id                   VARCHAR(36)  PRIMARY KEY DEFAULT gen_random_uuid(),
    parche_id            VARCHAR(36)  NOT NULL,
    user_id              VARCHAR(36)  NOT NULL,
    username             VARCHAR(100) NOT NULL,
    signaling_session_id VARCHAR(100),
    joined_at            TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    left_at              TIMESTAMPTZ,
    status               VARCHAR(20)  NOT NULL CHECK (status IN ('ACTIVE','DISCONNECTED'))
);

CREATE INDEX idx_voice_parche_user_status ON voice_sessions (parche_id, user_id, status);
CREATE INDEX idx_voice_parche_status      ON voice_sessions (parche_id, status);
