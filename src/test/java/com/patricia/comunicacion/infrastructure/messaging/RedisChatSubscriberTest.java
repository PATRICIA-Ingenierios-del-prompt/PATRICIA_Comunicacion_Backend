package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisChatSubscriber unit tests")
class RedisChatSubscriberTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private RedisChatSubscriber subscriber;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        subscriber = new RedisChatSubscriber(messagingTemplate, objectMapper);
    }

    private static org.springframework.data.redis.connection.Message redisMessage(
            byte[] channel, byte[] body) {
        return new org.springframework.data.redis.connection.Message() {
            @Override public byte[] getBody()    { return body; }
            @Override public byte[] getChannel() { return channel; }
        };
    }

    @Test
    @DisplayName("onMessage debería procesar mensaje sin lanzar excepción")
    void onMessage_shouldHandleMessageWithoutException() {
        String json = "{\"id\":\"msg-001\",\"parcheId\":\"parche-001\",\"senderId\":\"user-001\"," +
                "\"senderUsername\":\"david\",\"content\":\"Hola!\",\"fileUrl\":null," +
                "\"type\":\"TEXT\",\"sentAt\":\"2026-01-01T00:00:00Z\",\"deleted\":false}";

        var redisMsg = redisMessage("chat:parche-001".getBytes(), json.getBytes());
        subscriber.onMessage(redisMsg, null);
    }

    @Test
    @DisplayName("onMessage debería manejar JSON malformado sin lanzar excepción")
    void onMessage_shouldHandleMalformedJson() {
        var redisMsg = redisMessage("chat:parche-001".getBytes(), "invalid-json".getBytes());
        subscriber.onMessage(redisMsg, null);
        verifyNoInteractions(messagingTemplate);
    }
}