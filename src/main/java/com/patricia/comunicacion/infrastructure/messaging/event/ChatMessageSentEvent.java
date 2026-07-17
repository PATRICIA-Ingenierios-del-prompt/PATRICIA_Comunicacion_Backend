package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * Routing key: chat.message.sent
 * Consume: Notificaciones MS — decide si hay que mandar push a miembros offline.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ChatMessageSentEvent {
    private String messageId;
    private String parcheId;
    private String chatId;       // same as parcheId; kept for Notificaciones MS contract
    private String parcheName;   // display name: parche name for groups, "chat privado" for DMs
    private String senderId;
    private String senderUsername;
    /** Texto del mensaje o nombre del archivo para FILE/IMAGE. */
    private String content;
    /** null si type = TEXT o SYSTEM. */
    private String fileUrl;
    private String messageType;
    private Instant sentAt;
    /** Miembros del canal excluyendo el remitente; usado por Notificaciones. */
    private Set<String> recipientIds;
}
