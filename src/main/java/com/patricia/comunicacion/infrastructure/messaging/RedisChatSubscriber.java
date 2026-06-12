package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.infrastructure.web.dto.ChatMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Escucha los canales Redis chat:* y reenvía los mensajes a los
 * clientes WebSocket (STOMP) conectados a esta instancia del servicio.
 * Así el broadcast funciona con N réplicas corriendo en paralelo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message redisMsg, byte[] pattern) {
        try {
            Message message = objectMapper.readValue(new String(redisMsg.getBody()), Message.class);
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + message.getParcheId(),
                    ChatMessagePayload.fromDomain(message));
        } catch (Exception e) {
            log.error("Error procesando mensaje de Redis", e);
        }
    }
}
