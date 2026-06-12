package com.patricia.comunicacion.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberExpelledListener {

    private final ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "${kafka.topics.miembro-expulsado}",
            groupId          = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onMemberExpelled(String payload) {
        try {
            MiembroExpulsadoEvent event = objectMapper.readValue(payload, MiembroExpulsadoEvent.class);

            log.warn("miembro.expulsado recibido [parcheId={}, userId={}]",
                    event.parcheId(), event.userId());

            manageVoiceSessionUseCase.forceDisconnect(event.parcheId(), event.userId());

            messagingTemplate.convertAndSendToUser(
                    event.userId(),
                    "/queue/kicked",
                    Map.of("parcheId", event.parcheId(), "reason", "Fuiste expulsado del parche"));

        } catch (Exception e) {
            log.error("Error procesando evento miembro.expulsado: {}", payload, e);
        }
    }

    public record MiembroExpulsadoEvent(
            String parcheId,
            String userId,
            String expelledBy,
            String reason
    ) {}
}