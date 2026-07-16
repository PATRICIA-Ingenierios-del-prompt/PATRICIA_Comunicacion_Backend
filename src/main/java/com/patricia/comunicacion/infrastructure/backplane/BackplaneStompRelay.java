package com.patricia.comunicacion.infrastructure.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;

/**
 * Escucha el canal Redis del backplane y reenvía cada mensaje al broker
 * STOMP local del pod.
 *
 * Un mensaje malformado se descarta con WARN — nunca tumba el listener container.
 * Si el envelope trae targetUserId, se entrega como mensaje dirigido
 * (convertAndSendToUser) -- solo el pod donde vive la sesión STOMP de ese
 * usuario lo entrega de verdad; en los demás pods, Spring simplemente no
 * encuentra sesión local y no hace nada. Sin targetUserId, se trata como
 * broadcast normal (/topic/**).
 */
@Slf4j
public class BackplaneStompRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public BackplaneStompRelay(SimpMessagingTemplate messagingTemplate,
                               ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.objectMapper      = objectMapper;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            BackplaneEnvelope envelope = objectMapper.readValue(json, BackplaneEnvelope.class);

            if (envelope.destination() == null
                    || envelope.payload() == null
                    || envelope.payload().isNull()) {
                log.warn("Backplane message dropped: missing destination or payload");
                return;
            }

            // Convertir el JsonNode a su representación String para evitar que
            // SimpMessagingTemplate lo re-serialice envolviendo los campos en
            // una capa extra de JSON (lo que causa que el frontend reciba
            // signalType/senderUserId como undefined).
            String payloadStr = objectMapper.writeValueAsString(envelope.payload());

            if (envelope.targetUserId() != null) {
                messagingTemplate.convertAndSendToUser(
                        envelope.targetUserId(), envelope.destination(), payloadStr);
                log.debug("Backplane relayed to user [targetUserId={}, destination={}]",
                        envelope.targetUserId(), envelope.destination());
            } else {
                messagingTemplate.convertAndSend(envelope.destination(), payloadStr);
                log.debug("Backplane relayed [destination={}]", envelope.destination());
            }

        } catch (Exception ex) {
            log.warn("Failed to relay backplane message: {}", ex.getMessage());
        }
    }
}
