package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisMessageBrokerAdapter unit tests")
class RedisMessageBrokerAdapterTest {

    @Mock private StringRedisTemplate redisTemplate;

    private RedisMessageBrokerAdapter adapter;
    private ObjectMapper objectMapper;

    private static final String PARCHE_ID = "parche-001";
    private static final String USER_ID   = "user-001";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        adapter = new RedisMessageBrokerAdapter(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("publish debería serializar el mensaje y enviarlo al canal Redis correcto")
    void publish_shouldSendSerializedMessageToRedisChannel() {
        Message message = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("david").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        adapter.publish(PARCHE_ID, message);

        verify(redisTemplate).convertAndSend(eq("chat:" + PARCHE_ID), anyString());
    }

    @Test
    @DisplayName("publishForceDisconnect debería enviar payload al canal de desconexión")
    void publishForceDisconnect_shouldSendToDisconnectChannel() {
        adapter.publishForceDisconnect(PARCHE_ID, USER_ID);

        verify(redisTemplate).convertAndSend(eq("disconnect:" + PARCHE_ID), anyString());
    }

    @Test
    @DisplayName("publish debería manejar errores de Redis sin lanzar excepción")
    void publish_shouldHandleRedisErrorGracefully() {
        Message message = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("david").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        doThrow(new RuntimeException("Redis error")).when(redisTemplate)
                .convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> adapter.publish(PARCHE_ID, message));
    }

    @Test
    @DisplayName("publishForceDisconnect debería manejar errores de Redis sin lanzar excepción")
    void publishForceDisconnect_shouldHandleRedisErrorGracefully() {
        doThrow(new RuntimeException("Redis error")).when(redisTemplate)
                .convertAndSend(anyString(), anyString());

        assertDoesNotThrow(() -> adapter.publishForceDisconnect(PARCHE_ID, USER_ID));
    }
}
