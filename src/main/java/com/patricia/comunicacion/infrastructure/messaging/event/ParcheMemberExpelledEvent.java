package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento recibido cuando el dueño expulsa a un miembro del parche.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcheMemberExpelledEvent {
    private UUID parcheId;
    private UUID memberId;
}
