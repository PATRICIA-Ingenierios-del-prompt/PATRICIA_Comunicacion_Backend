package com.patricia.comunicacion.domain.port.out;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.VoiceSession;

/**
 * Puerto de salida para eventos que Comunicación publica al exchange
 * "comunicacion.events", dirigidos principalmente a Notificaciones.
 */
public interface EventPublisher {

    // ── Chat ─────────────────────────────────────────────────────────

    /**
     * Avisa que se envió un mensaje — Notificaciones decide si hay
     * que mandar push a los miembros que no están conectados.
     * Routing key: chat.message.sent
     */
    void publishMessageSent(Message message);

    // ── Voz ──────────────────────────────────────────────────────────

    /**
     * Primera persona que entró al canal — la llamada acaba de iniciar.
     * Routing key: voice.call.started
     */
    void publishVoiceCallStarted(String parcheId, VoiceSession initiator);

    /**
     * Alguien nuevo se unió a una llamada ya activa.
     * Routing key: voice.participant.joined
     */
    void publishVoiceParticipantJoined(String parcheId, VoiceSession participant,
                                        int totalParticipants);

    /**
     * Alguien salió del canal.
     * Routing key: voice.participant.left
     */
    void publishVoiceParticipantLeft(String parcheId, String userId,
                                      int remainingParticipants);

    /**
     * Último participante salió — la llamada terminó.
     * Routing key: voice.call.ended
     */
    void publishVoiceCallEnded(String parcheId, int durationSeconds);
}
