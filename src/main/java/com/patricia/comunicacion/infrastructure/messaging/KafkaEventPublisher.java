package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.chat-pendiente}")
    private String chatPendienteTopic;

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

            String payload = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(chatPendienteTopic, message.getParcheId(), payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Error publicando chat.pendiente [messageId={}]",
                                    message.getId(), ex);
                        }
                    });

        } catch (Exception e) {
            log.error("Error serializando evento chat.pendiente", e);
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