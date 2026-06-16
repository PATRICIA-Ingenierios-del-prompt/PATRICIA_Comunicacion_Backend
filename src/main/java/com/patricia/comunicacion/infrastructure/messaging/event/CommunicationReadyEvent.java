package com.patricia.comunicacion.infrastructure.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Evento de respuesta: Comunicación lo publica cuando termina de aprovisionar
 * el chat y el canal de voz de un parche. Parches lo escucha en su cola
 * parches.communication.ready.queue y lo usa para llamar
 * Parche.assignCommunication(chatId, voiceId).
 * <p>
 * La forma de esta clase debe coincidir con
 * ingprompt.patricia.parches...event.CommunicationReadyEvent (mismos campos,
 * mismo tipo). El nombre del paquete puede diferir porque el converter en
 * ambos lados usa TypePrecedence.INFERRED.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationReadyEvent {
    private UUID parcheId;
    private UUID chatId;
    private UUID voiceId;
}
