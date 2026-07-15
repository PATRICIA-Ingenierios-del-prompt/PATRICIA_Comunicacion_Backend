package com.patricia.comunicacion.infrastructure.ws;

import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

/**
 * Sin este listener, un usuario que refresca la página, cierra la pestaña o
 * pierde la red mientras está en el canal de voz se queda registrado como
 * ACTIVE en `voice_sessions` para siempre -- el único camino que existía
 * para desactivar la fila era el mensaje explícito /app/voice.leave, que
 * nunca se manda en una desconexión abrupta.
 *
 * Efecto observado sin este fix: GET /api/voice/{parcheId}/participants
 * acumula filas "fantasma" del mismo usuario, y por cada una el frontend
 * manda una OFFER nueva al mismo participante real, pisándose entre sí y
 * rompiendo la negociación WebRTC (se ven conectados pero nunca se
 * escuchan).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceSessionDisconnectListener {

    private final VoiceSessionTracker tracker;
    private final ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    private final ComunicacionBroadcaster broadcaster;

    private static final String VOICE_TOPIC = "/topic/voice/";

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String stompSessionId = accessor.getSessionId();
        if (stompSessionId == null) return;

        tracker.find(stompSessionId).ifPresent(membership -> {
            String parcheId = membership.parcheId();
            String userId = membership.userId();

            log.info("WS desconectado sin /app/voice.leave -- limpiando canal de voz "
                    + "[parcheId={}, userId={}, stompSessionId={}]", parcheId, userId, stompSessionId);

            manageVoiceSessionUseCase.leaveVoiceChannel(parcheId, userId);
            tracker.unregister(stompSessionId);

            broadcaster.broadcast(VOICE_TOPIC + parcheId, java.util.Map.of(
                    "signalType", "LEAVE",
                    "senderUserId", userId
            ));
        });
    }
}
