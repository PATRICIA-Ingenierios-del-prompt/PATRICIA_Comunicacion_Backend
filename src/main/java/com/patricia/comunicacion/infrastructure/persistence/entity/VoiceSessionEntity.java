package com.patricia.comunicacion.infrastructure.persistence.entity;

import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "voice_sessions",
    indexes = {
        @Index(name = "idx_voice_parche_user_status", columnList = "parche_id, user_id, status"),
        @Index(name = "idx_voice_parche_status",      columnList = "parche_id, status")
    }
)
@Getter @Setter
public class VoiceSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "parche_id", nullable = false)
    private String parcheId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String username;

    @Column(name = "signaling_session_id")
    private String signalingSessionId;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VoiceSessionStatus status;
}
