package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento recibido cuando Parches crea un parche nuevo.
 * Forma del JSON idéntica a ingprompt.patricia.parches...ParcheCreatedEvent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParcheCreatedEvent {
    private UUID sourceEventId;
    private UUID parcheId;
    private String name;
    private String visibility;
    private UUID ownerId;
}
