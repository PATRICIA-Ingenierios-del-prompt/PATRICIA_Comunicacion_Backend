package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento recibido cuando alguien se une a un parche existente.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcheMemberJoinedEvent {
    private UUID parcheId;
    private UUID memberId;
}
