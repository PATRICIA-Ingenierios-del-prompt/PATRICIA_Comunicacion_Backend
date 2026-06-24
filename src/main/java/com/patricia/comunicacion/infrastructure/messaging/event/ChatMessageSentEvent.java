package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Routing key: chat.message.sent
 * Consume: Notificaciones MS — decide si hay que mandar push a miembros offline.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ChatMessageSentEvent {
    private String messageId;
    private String parcheId;
    private String senderId;
    private String senderUsername;
    /** Texto del mensaje o nombre del archivo para FILE/IMAGE. */
    private String content;
    /** null si type = TEXT o SYSTEM. */
    private String fileUrl;
    private String messageType;
    private Instant sentAt;
}
