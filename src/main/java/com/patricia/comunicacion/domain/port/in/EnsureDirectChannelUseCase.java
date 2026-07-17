package com.patricia.comunicacion.domain.port.in;

/**
 * Chat privado 1 a 1 (DM). No crea un sistema de mensajería aparte:
 * devuelve un channelId determinístico (siempre el mismo para el mismo par
 * de usuarios) que de ahí en adelante se usa exactamente igual que un
 * parcheId en el resto de la API — STOMP (/app/chat.send/{id},
 * /topic/chat/{id}) e historial (/api/chat/{id}/messages).
 */
public interface EnsureDirectChannelUseCase {

    /**
     * Obtiene (o crea) el canal privado entre requestingUserId y otherUserId,
     * registrando a ambos como miembros para que pasen la verificación de
     * membresía al enviar/leer mensajes.
     *
     * @return channelId del canal DM (UUID determinístico del par)
     */
    String execute(String requestingUserId, String otherUserId);
}
