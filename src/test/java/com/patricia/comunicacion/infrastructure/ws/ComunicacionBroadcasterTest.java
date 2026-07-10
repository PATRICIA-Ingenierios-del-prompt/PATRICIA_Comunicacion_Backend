package com.patricia.comunicacion.infrastructure.ws;

import com.patricia.comunicacion.infrastructure.backplane.RedisBackplanePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComunicacionBroadcaster unit tests")
class ComunicacionBroadcasterTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ObjectProvider<RedisBackplanePublisher> backplaneProvider;
    @Mock private RedisBackplanePublisher backplanePublisher;

    @Test
    @DisplayName("broadcast debería usar backplane cuando está disponible")
    void broadcast_shouldUseBackplaneWhenAvailable() {
        when(backplaneProvider.getIfAvailable()).thenReturn(backplanePublisher);

        ComunicacionBroadcaster broadcaster =
                new ComunicacionBroadcaster(messagingTemplate, backplaneProvider);

        broadcaster.broadcast("/topic/chat/parche-001", "payload");

        verify(backplanePublisher).publish("/topic/chat/parche-001", "payload");
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    @DisplayName("broadcast debería usar messagingTemplate local cuando backplane no está disponible")
    void broadcast_shouldUseLocalWhenBackplaneUnavailable() {
        when(backplaneProvider.getIfAvailable()).thenReturn(null);

        ComunicacionBroadcaster broadcaster =
                new ComunicacionBroadcaster(messagingTemplate, backplaneProvider);

        broadcaster.broadcast("/topic/chat/parche-001", "payload");

        verify(messagingTemplate).convertAndSend("/topic/chat/parche-001", "payload");
    }

    @Test
    @DisplayName("broadcast debería hacer fallback local cuando backplane falla")
    void broadcast_shouldFallbackToLocalWhenBackplaneFails() {
        when(backplaneProvider.getIfAvailable()).thenReturn(backplanePublisher);
        doThrow(new RuntimeException("Redis error"))
                .when(backplanePublisher).publish(anyString(), any());

        ComunicacionBroadcaster broadcaster =
                new ComunicacionBroadcaster(messagingTemplate, backplaneProvider);

        broadcaster.broadcast("/topic/chat/parche-001", "payload");

        verify(messagingTemplate).convertAndSend("/topic/chat/parche-001", "payload");
    }

    @Test
    @DisplayName("broadcast debería manejar errores del messagingTemplate local sin lanzar excepción")
    void broadcast_shouldHandleLocalBroadcastError() {
        when(backplaneProvider.getIfAvailable()).thenReturn(null);
        doThrow(new RuntimeException("STOMP error"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        ComunicacionBroadcaster broadcaster =
                new ComunicacionBroadcaster(messagingTemplate, backplaneProvider);

        assertDoesNotThrow(() -> broadcaster.broadcast("/topic/chat/parche-001", "payload"));
    }
}
