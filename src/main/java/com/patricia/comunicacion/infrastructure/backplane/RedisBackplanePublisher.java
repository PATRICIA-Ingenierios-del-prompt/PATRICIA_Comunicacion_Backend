package com.patricia.comunicacion.infrastructure.backplane;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Publica un mensaje en el canal Redis del backplane.
 *
 * Todos los pods suscritos (incluido el que publica) recibirán el mensaje
 * via BackplaneStompRelay y lo reenviarán a sus sesiones STOMP locales.
 * El pod publicador NO emite localmente — se recibe a sí mismo por Redis
 * para evitar duplicados.
 */
@Slf4j
public class RedisBackplanePublisher {

    private final StringRedisTemplate backplaneRedis;
    private final ObjectMapper objectMapper;
    private final String channel;

    public RedisBackplanePublisher(StringRedisTemplate backplaneRedis,
                                   ObjectMapper objectMapper,
                                   String channel) {
        this.backplaneRedis = backplaneRedis;
        this.objectMapper   = objectMapper;
        this.channel        = channel;
    }

    /**
     * Serializa payload como BackplaneEnvelope y lo publica en Redis.
     *
     * @param destination destino STOMP completo ("/topic/chat/{parcheId}", etc.)
     * @param payload     objeto a serializar — debe ser serializable por Jackson
     */
    public void publish(String destination, Object payload) {
        try {
            BackplaneEnvelope envelope = new BackplaneEnvelope(
                    destination,
                    objectMapper.valueToTree(payload));
            backplaneRedis.convertAndSend(channel, objectMapper.writeValueAsString(envelope));
            log.debug("Backplane published [channel={}, destination={}]", channel, destination);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(
                    "Backplane payload is not serializable: " + payload.getClass(), e);
        }
    }
}
