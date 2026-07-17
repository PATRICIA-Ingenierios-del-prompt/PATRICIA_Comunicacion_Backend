package com.patricia.comunicacion.infrastructure.messaging;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.infrastructure.config.RabbitMQConfig;
import com.patricia.comunicacion.infrastructure.messaging.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

/**
 * Publica todos los eventos de Comunicación al exchange "comunicacion.events".
 * Los consumidores típicos son Notificaciones y futuros microservicios de analítica.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    // ── Chat ─────────────────────────────────────────────────────────

    @Override
    public void publishMessageSent(Message message, Set<String> recipientIds, String chatName) {
        try {
            var event = new ChatMessageSentEvent(
                    message.getId(),
                    message.getParcheId(),
                    message.getParcheId(),
                    chatName,
                    message.getSenderId(),
                    message.getSenderUsername(),
                    message.getContent(),
                    message.getFileUrl(),
                    message.getType().name(),
                    message.getSentAt(),
                    recipientIds
            );
            publish(RabbitMQConfig.CHAT_MESSAGE_SENT_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Error publicando chat.message.sent [messageId={}]", message.getId(), e);
        }
    }

    // ── Voz ──────────────────────────────────────────────────────────

    @Override
    public void publishVoiceCallStarted(String parcheId, VoiceSession initiator) {
        try {
            var event = new VoiceCallStartedEvent(
                    parcheId,
                    initiator.getUserId(),
                    initiator.getUsername(),
                    Instant.now()
            );
            publish(RabbitMQConfig.VOICE_CALL_STARTED_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Error publicando voice.call.started [parcheId={}]", parcheId, e);
        }
    }

    @Override
    public void publishVoiceParticipantJoined(String parcheId, VoiceSession participant,
                                               int totalParticipants) {
        try {
            var event = new VoiceParticipantJoinedEvent(
                    parcheId,
                    participant.getUserId(),
                    participant.getUsername(),
                    totalParticipants
            );
            publish(RabbitMQConfig.VOICE_PARTICIPANT_JOINED_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Error publicando voice.participant.joined [parcheId={}]", parcheId, e);
        }
    }

    @Override
    public void publishVoiceParticipantLeft(String parcheId, String userId,
                                             int remainingParticipants) {
        try {
            var event = new VoiceParticipantLeftEvent(parcheId, userId, remainingParticipants);
            publish(RabbitMQConfig.VOICE_PARTICIPANT_LEFT_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Error publicando voice.participant.left [parcheId={}]", parcheId, e);
        }
    }

    @Override
    public void publishVoiceCallEnded(String parcheId, int durationSeconds) {
        try {
            var event = new VoiceCallEndedEvent(parcheId, Instant.now(), durationSeconds);
            publish(RabbitMQConfig.VOICE_CALL_ENDED_ROUTING_KEY, event);
        } catch (Exception e) {
            log.error("Error publicando voice.call.ended [parcheId={}]", parcheId, e);
        }
    }

    // ── Helper ───────────────────────────────────────────────────────

    private void publish(String routingKey, Object event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.COMUNICACION_EXCHANGE, routingKey, event);
        log.debug("Evento publicado [exchange={}, key={}]",
                RabbitMQConfig.COMUNICACION_EXCHANGE, routingKey);
    }
}
