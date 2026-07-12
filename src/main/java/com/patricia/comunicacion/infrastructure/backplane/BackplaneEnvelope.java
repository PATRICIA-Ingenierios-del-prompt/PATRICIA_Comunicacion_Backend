package com.patricia.comunicacion.infrastructure.backplane;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Envoltura que viaja por el canal Redis del backplane.
 *
 * destination → destino STOMP completo, ej. "/topic/chat/parcheId"
 * payload     → body exacto que recibe el cliente frontend (sin transformar)
 */
public record BackplaneEnvelope(String destination, JsonNode payload) {
}
