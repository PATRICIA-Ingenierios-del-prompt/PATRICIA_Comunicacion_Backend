package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Routing key: voice.participant.joined */
@Data @NoArgsConstructor @AllArgsConstructor
public class VoiceParticipantJoinedEvent {
    private String parcheId;
    private String userId;
    private String username;
    private int totalParticipants;
}
