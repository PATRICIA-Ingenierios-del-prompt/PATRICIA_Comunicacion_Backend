package com.patricia.comunicacion.infrastructure.web.dto;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePayload {

    private String content;
    private MessageType type;

    /**
     * URL del archivo — obligatorio cuando type = FILE o IMAGE.
     * El cliente sube el archivo previamente al storage y pasa la URL aquí.
     */
    private String fileUrl;

    // Campos de respuesta (los rellena el servidor al hacer broadcast)
    private String messageId;
    private String senderId;
    private String senderUsername;
    private String parcheId;
    private Instant sentAt;
    private Set<String> readBy;

    public static ChatMessagePayload fromDomain(Message m) {
        return ChatMessagePayload.builder()
                .messageId(m.getId())
                .content(m.getContent())
                .fileUrl(m.getFileUrl())
                .type(m.getType())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .parcheId(m.getParcheId())
                .sentAt(m.getSentAt())
                .readBy(m.getReadBy())
                .build();
    }
}
