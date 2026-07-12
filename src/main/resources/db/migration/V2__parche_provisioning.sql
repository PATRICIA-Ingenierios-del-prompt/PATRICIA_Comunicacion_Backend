-- ================================================================
-- V2__parche_provisioning.sql
-- Soporte para integración por eventos con Parches Core (RabbitMQ)
-- ================================================================

-- Registro de aprovisionamiento: chatId/voiceId generados por parche.
-- Sirve también de guardia de idempotencia ante reentregas de RabbitMQ.
CREATE TABLE parche_channel (
    parche_id  VARCHAR(36)  PRIMARY KEY,
    chat_id    VARCHAR(36)  NOT NULL,
    voice_id   VARCHAR(36)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Caché local de membresía, alimentada por los eventos parche.created,
-- parche.member.joined y parche.member.expelled. Reemplaza la verificación
-- síncrona por HTTP que llamaba a un endpoint de Parches inexistente.
CREATE TABLE parche_member (
    id        VARCHAR(36) PRIMARY KEY DEFAULT gen_random_uuid(),
    parche_id VARCHAR(36) NOT NULL,
    user_id   VARCHAR(36) NOT NULL
);

CREATE UNIQUE INDEX idx_parche_member_lookup ON parche_member (parche_id, user_id);
