package com.patricia.comunicacion.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.Set;

@Value
@Builder
@With
public class Message {

    String id;
    String parcheId;
    String senderId;
    String senderUsername;
    String content;
    MessageType type;
    Instant sentAt;
    Set<String> readBy;
    boolean deleted;

    public static Message systemMessage(String parcheId, String content) {
        return Message.builder()
                .parcheId(parcheId)
                .senderId("SYSTEM")
                .senderUsername("PATRICI.A")
                .content(content)
                .type(MessageType.SYSTEM)
                .sentAt(Instant.now())
                .deleted(false)
                .build();
    }
}
