package com.patricia.comunicacion.infrastructure.persistence.entity;

import com.patricia.comunicacion.domain.model.MessageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "messages",
    indexes = {
        @Index(name = "idx_messages_parche_sent", columnList = "parche_id, sent_at DESC"),
        @Index(name = "idx_messages_sender",      columnList = "sender_id")
    }
)
@Getter @Setter
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "parche_id", nullable = false)
    private String parcheId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "sender_username", nullable = false)
    private String senderUsername;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "message_read_by",
        joinColumns = @JoinColumn(name = "message_id")
    )
    @Column(name = "user_id")
    private Set<String> readBy = new HashSet<>();

    @Column(nullable = false)
    private boolean deleted = false;
}
