package com.patricia.comunicacion.infrastructure.ws;

import com.patricia.comunicacion.infrastructure.ws.VoiceSessionTracker.VoiceMembership;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("VoiceSessionTracker unit tests")
class VoiceSessionTrackerTest {

    private final VoiceSessionTracker tracker = new VoiceSessionTracker();

    @Test
    @DisplayName("register + find debería devolver la membresía registrada")
    void register_thenFind_returnsMembership() {
        tracker.register("stomp-001", "parche-A", "user-1");

        Optional<VoiceMembership> result = tracker.find("stomp-001");

        assertTrue(result.isPresent());
        assertEquals("parche-A", result.get().parcheId());
        assertEquals("user-1", result.get().userId());
    }

    @Test
    @DisplayName("find con sessionId no registrado debería devolver vacío")
    void find_unknownSession_returnsEmpty() {
        assertFalse(tracker.find("no-existe").isPresent());
    }

    @Test
    @DisplayName("unregister debería eliminar la membresía")
    void unregister_thenFind_returnsEmpty() {
        tracker.register("stomp-002", "parche-B", "user-2");

        tracker.unregister("stomp-002");

        assertFalse(tracker.find("stomp-002").isPresent());
    }

    @Test
    @DisplayName("register con el mismo sessionId debería sobreescribir la membresía anterior")
    void register_sameTwice_overwritesPrevious() {
        tracker.register("stomp-003", "parche-C", "user-3");
        tracker.register("stomp-003", "parche-D", "user-4");

        VoiceMembership result = tracker.find("stomp-003").orElseThrow();
        assertEquals("parche-D", result.parcheId());
        assertEquals("user-4", result.userId());
    }
}
