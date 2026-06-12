package com.patricia.comunicacion.domain.model;

import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;

@Value
@Builder
@With
public class VoiceSession {

    String id;
    String parcheId;
    String userId;
    String username;
    String signalingSessionId;
    Instant joinedAt;
    Instant leftAt;
    VoiceSessionStatus status;

    public VoiceSession disconnect() {
        return this.withStatus(VoiceSessionStatus.DISCONNECTED).withLeftAt(Instant.now());
    }
}
