package com.patricia.comunicacion.infrastructure.messaging;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.infrastructure.config.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RabbitEventPublisher unit tests")
class RabbitEventPublisherTest {

    @Mock private RabbitTemplate rabbitTemplate;

    private RabbitEventPublisher publisher;

    private static final String PARCHE_ID = "parche-001";

    @BeforeEach
    void setUp() {
        publisher = new RabbitEventPublisher(rabbitTemplate);
    }

    @Test
    @DisplayName("publishMessageSent debería publicar en el exchange y routing key correctos")
    void publishMessageSent_shouldPublishToCorrectExchange() {
        Message message = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId("user-001")
                .senderUsername("david").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        publisher.publishMessageSent(message);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.COMUNICACION_EXCHANGE),
                eq(RabbitMQConfig.CHAT_MESSAGE_SENT_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publishVoiceCallStarted debería publicar evento de inicio de llamada")
    void publishVoiceCallStarted_shouldPublishEvent() {
        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId("user-001")
                .username("karol").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        publisher.publishVoiceCallStarted(PARCHE_ID, session);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.COMUNICACION_EXCHANGE),
                eq(RabbitMQConfig.VOICE_CALL_STARTED_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publishVoiceParticipantJoined debería publicar evento de participante unido")
    void publishVoiceParticipantJoined_shouldPublishEvent() {
        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId("user-001")
                .username("karol").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        publisher.publishVoiceParticipantJoined(PARCHE_ID, session, 2);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.COMUNICACION_EXCHANGE),
                eq(RabbitMQConfig.VOICE_PARTICIPANT_JOINED_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publishVoiceParticipantLeft debería publicar evento de participante saliente")
    void publishVoiceParticipantLeft_shouldPublishEvent() {
        publisher.publishVoiceParticipantLeft(PARCHE_ID, "user-001", 1);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.COMUNICACION_EXCHANGE),
                eq(RabbitMQConfig.VOICE_PARTICIPANT_LEFT_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publishVoiceCallEnded debería publicar evento de fin de llamada")
    void publishVoiceCallEnded_shouldPublishEvent() {
        publisher.publishVoiceCallEnded(PARCHE_ID, 120);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.COMUNICACION_EXCHANGE),
                eq(RabbitMQConfig.VOICE_CALL_ENDED_ROUTING_KEY),
                any(Object.class));
    }

    @Test
    @DisplayName("publishMessageSent debería manejar errores sin lanzar excepción")
    void publishMessageSent_shouldHandleErrorGracefully() {
        Message message = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId("user-001")
                .senderUsername("david").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        doThrow(new RuntimeException("RabbitMQ error")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> publisher.publishMessageSent(message));
    }
}
