package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

/**
 * Evento recibido cuando el dueño borra un parche.
 * Comunicación debe limpiar chat y canal de voz asociados.
 */
@Data @NoArgsConstructor @AllArgsConstructor
public class ParcheDeletedEvent {
    private UUID parcheId;
    private UUID ownerId;
    private Set<UUID> eventIds;
}
