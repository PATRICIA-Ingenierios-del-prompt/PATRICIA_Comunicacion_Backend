package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageBrokerAdapter implements MessageBroker {

    private static final String CHAT_PREFIX       = "chat:";
    private static final String DISCONNECT_PREFIX = "disconnect:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(String parcheId, Message message) {
        try {
            redisTemplate.convertAndSend(CHAT_PREFIX + parcheId,
                    objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            log.error("Error publicando en Redis [parcheId={}]", parcheId, e);
        }
    }

    @Override
    public void publishForceDisconnect(String parcheId, String userId) {
        try {
            redisTemplate.convertAndSend(DISCONNECT_PREFIX + parcheId,
                    objectMapper.writeValueAsString(new ForceDisconnectPayload(parcheId, userId)));
        } catch (Exception e) {
            log.error("Error publicando desconexión forzada [parcheId={}, userId={}]", parcheId, userId, e);
        }
    }

    public record ForceDisconnectPayload(String parcheId, String userId) {}
}
