package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.infrastructure.web.dto.ChatMessagePayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Regresión: el broadcast de chat (RF-PAR-04) viaja por Redis Pub/Sub y se
 * (de)serializa con el ObjectMapper de RedisConfig. El modelo de dominio
 * Message es inmutable (@Value) y Jackson no puede reconstruirlo sin un
 * creador; por eso el contrato de cable es ChatMessagePayload, un DTO mutable.
 * Este test prueba el viaje completo publish → onMessage con ese ObjectMapper.
 */
@DisplayName("Redis chat broadcast round-trip")
class RedisChatBroadcastTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    @DisplayName("publish serializa un payload que onMessage puede deserializar y reenviar por STOMP")
    void publishThenSubscribe_deliversMessageToStompTopic() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);

        RedisMessageBrokerAdapter broker =
                new RedisMessageBrokerAdapter(redisTemplate, objectMapper);
        RedisChatSubscriber subscriber =
                new RedisChatSubscriber(messagingTemplate, objectMapper);

        Message message = Message.builder()
                .id("msg-1").parcheId("p-1").senderId("u-1")
                .senderUsername("karol").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        broker.publish("p-1", message);

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq("chat:p-1"), payload.capture());

        subscriber.onMessage(
                new DefaultMessage("chat:p-1".getBytes(StandardCharsets.UTF_8),
                        payload.getValue().getBytes(StandardCharsets.UTF_8)),
                null);

        ArgumentCaptor<ChatMessagePayload> delivered =
                ArgumentCaptor.forClass(ChatMessagePayload.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/p-1"), delivered.capture());

        ChatMessagePayload result = delivered.getValue();
        assertThat(result.getMessageId()).isEqualTo("msg-1");
        assertThat(result.getContent()).isEqualTo("Hola!");
        assertThat(result.getSenderUsername()).isEqualTo("karol");
        assertThat(result.getType()).isEqualTo(MessageType.TEXT);
    }
}
