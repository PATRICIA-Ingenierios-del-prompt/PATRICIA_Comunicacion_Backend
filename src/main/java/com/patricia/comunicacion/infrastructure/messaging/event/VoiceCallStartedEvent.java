package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Routing key: voice.call.started */
@Data @NoArgsConstructor @AllArgsConstructor
public class VoiceCallStartedEvent {
    private String parcheId;
    private String initiatorUserId;
    private String initiatorUsername;
    private Instant startedAt;
}
