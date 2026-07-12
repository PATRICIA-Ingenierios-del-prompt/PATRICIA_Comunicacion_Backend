-- ================================================================
-- V3__message_file_support.sql
-- Soporte para mensajes con archivo adjunto (FILE e IMAGE)
-- ================================================================

-- Columna para la URL del archivo subido (null para mensajes de texto)
ALTER TABLE messages ADD COLUMN file_url TEXT;

-- Actualiza el CHECK para reflejar los tipos actuales del enum
-- (ya incluía FILE e IMAGE desde V1, sin cambios necesarios en el CHECK)
