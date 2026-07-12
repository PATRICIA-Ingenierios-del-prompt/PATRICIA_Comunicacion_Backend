package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Routing key: voice.call.ended */
@Data @NoArgsConstructor @AllArgsConstructor
public class VoiceCallEndedEvent {
    private String parcheId;
    private Instant endedAt;
    /** Duración aproximada en segundos de la sesión. */
    private int durationSeconds;
}
