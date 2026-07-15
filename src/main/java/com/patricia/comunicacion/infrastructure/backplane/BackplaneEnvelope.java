package com.patricia.comunicacion.infrastructure.backplane;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envoltura que viaja por el canal Redis del backplane.
 *
 * destination   → destino STOMP. Para broadcasts: el topic completo, ej.
 *                 "/topic/chat/parcheId". Para mensajes dirigidos a un
 *                 usuario: el destino relativo que espera
 *                 convertAndSendToUser, ej. "/queue/voice-signal".
 * payload       → body exacto que recibe el cliente frontend (sin transformar)
 * targetUserId  → null para broadcasts (/topic/**). Si viene seteado, el
 *                 mensaje es un envío dirigido (equivalente a
 *                 convertAndSendToUser) y cada pod debe intentar entregarlo
 *                 solo a la sesión local de ese usuario -- en los pods donde
 *                 no vive esa sesión, la entrega simplemente no hace nada.
 */
public record BackplaneEnvelope(String destination, JsonNode payload, String targetUserId) {
}
