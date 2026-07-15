package com.patricia.comunicacion.infrastructure.ws;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Asocia cada sessionId de STOMP con el parche/usuario de voz al que se unió.
 *
 * Necesario porque un {@code SessionDisconnectEvent} solo trae el sessionId
 * -- no sabe a qué parche pertenecía esa sesión de voz -- así que sin este
 * registro no habría forma de saber qué VoiceSession hay que desactivar
 * cuando alguien se desconecta sin mandar /app/voice.leave (cierre de
 * pestaña, refresh, caída de red, etc.).
 *
 * Vive en memoria de cada pod: si el pod se reinicia se pierde el registro,
 * pero para entonces todas sus sesiones STOMP locales también murieron, así
 * que no hay inconsistencia que arrastrar.
 */
@Component
public class VoiceSessionTracker {

    public record VoiceMembership(String parcheId, String userId) {}

    private final Map<String, VoiceMembership> byStompSessionId = new ConcurrentHashMap<>();

    public void register(String stompSessionId, String parcheId, String userId) {
        byStompSessionId.put(stompSessionId, new VoiceMembership(parcheId, userId));
    }

    public void unregister(String stompSessionId) {
        byStompSessionId.remove(stompSessionId);
    }

    public Optional<VoiceMembership> find(String stompSessionId) {
        return Optional.ofNullable(byStompSessionId.get(stompSessionId));
    }
}
