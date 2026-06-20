package com.patricia.comunicacion.infrastructure.messaging;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.infrastructure.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publica chat.pendiente hacia Notificaciones. Antes salía por Kafka; ahora
 * que toda la mensajería de PATRICI.A corre sobre RabbitMQ, usa el mismo
 * broker — mismo nombre de evento y misma forma de payload, solo cambió el
 * transporte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitChatEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishChatPendiente(Message message) {
        try {
            var event = new ChatPendienteEvent(
                    message.getId(),
                    message.getParcheId(),
                    message.getSenderId(),
                    message.getSenderUsername(),
                    message.getContent(),
                    message.getType().name(),
                    message.getSentAt()
            );

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COMUNICACION_EXCHANGE,
                    RabbitMQConfig.CHAT_PENDIENTE_ROUTING_KEY,
                    event);

        } catch (Exception e) {
            log.error("Error publicando chat.pendiente [messageId={}]", message.getId(), e);
        }
    }

    public record ChatPendienteEvent(
            String messageId,
            String parcheId,
            String senderId,
            String senderUsername,
            String contentPreview,
            String messageType,
            Instant sentAt
    ) {}
}
