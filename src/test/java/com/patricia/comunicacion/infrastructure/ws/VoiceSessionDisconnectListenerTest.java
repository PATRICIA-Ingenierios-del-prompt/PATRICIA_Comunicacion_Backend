package com.patricia.comunicacion.infrastructure.ws;

import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.infrastructure.ws.VoiceSessionTracker.VoiceMembership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoiceSessionDisconnectListener unit tests")
class VoiceSessionDisconnectListenerTest {

    @Mock private VoiceSessionTracker tracker;
    @Mock private ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    @Mock private ComunicacionBroadcaster broadcaster;

    @InjectMocks
    private VoiceSessionDisconnectListener listener;

    @Test
    @DisplayName("debería limpiar la sesión de voz cuando el usuario se desconecta sin /app/voice.leave")
    void onDisconnect_withTrackedSession_cleansUpVoiceSession() {
        SessionDisconnectEvent event = buildDisconnectEvent("stomp-abc");
        VoiceMembership membership = new VoiceMembership("parche-X", "user-42");
        when(tracker.find("stomp-abc")).thenReturn(Optional.of(membership));

        listener.onDisconnect(event);

        verify(manageVoiceSessionUseCase).leaveVoiceChannel("parche-X", "user-42");
        verify(tracker).unregister("stomp-abc");
        verify(broadcaster).broadcast(eq("/topic/voice/parche-X"), any());
    }

    @Test
    @DisplayName("no debería hacer nada si la sesión STOMP no estaba en un canal de voz")
    void onDisconnect_withUntrackedSession_doesNothing() {
        SessionDisconnectEvent event = buildDisconnectEvent("stomp-xyz");
        when(tracker.find("stomp-xyz")).thenReturn(Optional.empty());

        listener.onDisconnect(event);

        verifyNoInteractions(manageVoiceSessionUseCase);
        verifyNoInteractions(broadcaster);
        verify(tracker, never()).unregister(anyString());
    }

    @Test
    @DisplayName("no debería hacer nada si el sessionId es nulo")
    void onDisconnect_withNullSessionId_doesNothing() {
        SessionDisconnectEvent event = buildDisconnectEvent(null);

        listener.onDisconnect(event);

        verifyNoInteractions(tracker);
        verifyNoInteractions(manageVoiceSessionUseCase);
        verifyNoInteractions(broadcaster);
    }

    @SuppressWarnings("unchecked")
    private SessionDisconnectEvent buildDisconnectEvent(String stompSessionId) {
        Map<String, Object> headers = new java.util.HashMap<>();
        if (stompSessionId != null) {
            headers.put(SimpMessageHeaderAccessor.SESSION_ID_HEADER, stompSessionId);
        }
        Message<byte[]> message = mock(Message.class);
        when(message.getHeaders()).thenReturn(new MessageHeaders(headers));
        when(message.getPayload()).thenReturn(new byte[0]);
        return new SessionDisconnectEvent(this, message, stompSessionId, null);
    }
}
