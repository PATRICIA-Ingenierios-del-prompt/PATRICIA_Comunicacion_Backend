package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Routing key: voice.participant.left */
@Data @NoArgsConstructor @AllArgsConstructor
public class VoiceParticipantLeftEvent {
    private String parcheId;
    private String userId;
    private int remainingParticipants;
}
