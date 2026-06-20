package com.patricia.comunicacion.infrastructure.web.dto;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(max = 2000)
    private String content;

    private MessageType type;
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
                .type(m.getType())
                .senderId(m.getSenderId())
                .senderUsername(m.getSenderUsername())
                .parcheId(m.getParcheId())
                .sentAt(m.getSentAt())
                .readBy(m.getReadBy())
                .build();
    }
}
