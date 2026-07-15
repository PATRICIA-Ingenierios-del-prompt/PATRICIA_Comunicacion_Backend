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
        publishInternal(destination, payload, null);
    }

    /**
     * Igual que {@link #publish}, pero para un envío dirigido a un usuario
     * específico (equivalente a {@code convertAndSendToUser}). Todos los pods
     * reciben el mensaje por Redis; solo el pod donde vive la sesión STOMP de
     * ese usuario lo entrega, los demás lo descartan sin efecto.
     *
     * @param targetUserId  id del usuario destino (claim "sub" del JWT)
     * @param destination   destino relativo, ej. "/queue/voice-signal"
     * @param payload       objeto a serializar
     */
    public void publishToUser(String targetUserId, String destination, Object payload) {
        publishInternal(destination, payload, targetUserId);
    }

    private void publishInternal(String destination, Object payload, String targetUserId) {
        try {
            BackplaneEnvelope envelope = new BackplaneEnvelope(
                    destination,
                    objectMapper.valueToTree(payload),
                    targetUserId);
            backplaneRedis.convertAndSend(channel, objectMapper.writeValueAsString(envelope));
            log.debug("Backplane published [channel={}, destination={}, targetUserId={}]",
                    channel, destination, targetUserId);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException(
                    "Backplane payload is not serializable: " + payload.getClass(), e);
        }
    }
}
