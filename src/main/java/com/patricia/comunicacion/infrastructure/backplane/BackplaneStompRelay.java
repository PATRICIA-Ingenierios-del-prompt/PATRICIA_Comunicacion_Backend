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
 * Solo se retransmiten broadcasts (/topic/**); los mensajes dirigidos a una
 * sesión (/user/queue/**) NO pasan por el backplane (ver spec sección 5).
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

            messagingTemplate.convertAndSend(envelope.destination(), envelope.payload());
            log.debug("Backplane relayed [destination={}]", envelope.destination());

        } catch (Exception ex) {
            log.warn("Failed to relay backplane message: {}", ex.getMessage());
        }
    }
}
