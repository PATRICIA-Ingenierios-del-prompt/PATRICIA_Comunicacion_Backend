package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.in.SendMessageUseCase;
import com.patricia.comunicacion.infrastructure.web.dto.ChatMessagePayload;
import com.patricia.comunicacion.infrastructure.web.dto.VoiceSignalPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

/**
 * Controladores STOMP para chat (RF-PAR-04) y voz (RF-PAR-05).
 *
 * Destinos del cliente:
 *   /app/chat.send/{parcheId}    — enviar mensaje
 *   /app/chat.read/{parcheId}    — marcar mensajes como leídos
 *   /app/voice.join/{parcheId}   — unirse al canal de voz
 *   /app/voice.signal/{parcheId} — relay señalización WebRTC
 *   /app/voice.leave/{parcheId}  — salir del canal de voz
 *
 * Suscripciones del cliente:
 *   /topic/chat/{parcheId}        — mensajes broadcast
 *   /topic/voice/{parcheId}       — eventos de voz (join/leave)
 *   /user/queue/voice-signal      — señales WebRTC punto a punto
 *   /user/queue/kicked            — notificación de expulsión
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ComunicacionWebSocketController {

    private final SendMessageUseCase sendMessageUseCase;
    private final ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    private final SimpMessagingTemplate messagingTemplate;

    // ── Chat ────────────────────────────────────────────────────────

    @MessageMapping("/chat.send/{parcheId}")
    public void sendMessage(
            @DestinationVariable String parcheId,
            @Payload ChatMessagePayload payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId   = getAttribute(headerAccessor, "userId");
        String username = getAttribute(headerAccessor, "username");
        MessageType type = payload.getType() != null ? payload.getType() : MessageType.TEXT;

        if (payload.getFileUrl() != null && !payload.getFileUrl().isBlank()) {
            sendMessageUseCase.executeWithFile(parcheId, userId, username,
                    payload.getContent(), type, payload.getFileUrl());
        } else {
            sendMessageUseCase.execute(parcheId, userId, username, payload.getContent(), type);
        }
    }

    @MessageMapping("/chat.read/{parcheId}")
    public void markAsRead(
            @DestinationVariable String parcheId,
            SimpMessageHeaderAccessor headerAccessor) {

        sendMessageUseCase.markAsRead(parcheId, getAttribute(headerAccessor, "userId"));
    }

    // ── Voz (RF-PAR-05) ─────────────────────────────────────────────

    @MessageMapping("/voice.join/{parcheId}")
    public void joinVoice(
            @DestinationVariable String parcheId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId   = getAttribute(headerAccessor, "userId");
        String username = getAttribute(headerAccessor, "username");

        manageVoiceSessionUseCase.joinVoiceChannel(parcheId, userId, username,
                UUID.randomUUID().toString());

        messagingTemplate.convertAndSend("/topic/voice/" + parcheId,
                VoiceSignalPayload.builder()
                        .signalType(VoiceSignalPayload.SignalType.JOIN)
                        .senderUserId(userId)
                        .senderUsername(username)
                        .build());
    }

    @MessageMapping("/voice.signal/{parcheId}")
    public void relaySignal(
            @DestinationVariable String parcheId,
            @Payload VoiceSignalPayload signal,
            SimpMessageHeaderAccessor headerAccessor) {

        signal.setSenderUserId(getAttribute(headerAccessor, "userId"));

        if (signal.getTargetUserId() != null) {
            messagingTemplate.convertAndSendToUser(
                    signal.getTargetUserId(), "/queue/voice-signal", signal);
        } else {
            messagingTemplate.convertAndSend("/topic/voice/" + parcheId, signal);
        }
    }

    @MessageMapping("/voice.leave/{parcheId}")
    public void leaveVoice(
            @DestinationVariable String parcheId,
            SimpMessageHeaderAccessor headerAccessor) {

        String userId   = getAttribute(headerAccessor, "userId");
        String username = getAttribute(headerAccessor, "username");

        manageVoiceSessionUseCase.leaveVoiceChannel(parcheId, userId);

        messagingTemplate.convertAndSend("/topic/voice/" + parcheId,
                VoiceSignalPayload.builder()
                        .signalType(VoiceSignalPayload.SignalType.LEAVE)
                        .senderUserId(userId)
                        .senderUsername(username)
                        .build());
    }

    private String getAttribute(SimpMessageHeaderAccessor accessor, String key) {
        return (String) accessor.getSessionAttributes().get(key);
    }
}
