package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VoiceSessionController unit tests")
class VoiceSessionControllerTest {

    @Mock private ManageVoiceSessionUseCase manageVoiceSessionUseCase;

    private MockMvc mockMvc;

    private static final String PARCHE_ID = "parche-001";
    private static final String USER_ID   = "user-001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new VoiceSessionController(manageVoiceSessionUseCase))
                .build();
    }

    @Test
    @DisplayName("GET /api/voice/{parcheId}/participants debería retornar participantes activos")
    void getActiveParticipants_shouldReturn200WithParticipants() throws Exception {
        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("david").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(manageVoiceSessionUseCase.getActiveParticipants(PARCHE_ID))
                .thenReturn(List.of(session));

        mockMvc.perform(get("/api/voice/{parcheId}/participants", PARCHE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(USER_ID))
                .andExpect(jsonPath("$[0].username").value("david"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/voice/{parcheId}/participants debería retornar lista vacía")
    void getActiveParticipants_shouldReturn200WithEmptyList() throws Exception {
        when(manageVoiceSessionUseCase.getActiveParticipants(PARCHE_ID))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/voice/{parcheId}/participants", PARCHE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("DELETE /api/voice/{parcheId}/participants/{userId} debería retornar 204")
    void kickFromVoice_shouldReturn204() throws Exception {
        doNothing().when(manageVoiceSessionUseCase).forceDisconnect(PARCHE_ID, USER_ID);

        mockMvc.perform(delete("/api/voice/{parcheId}/participants/{userId}", PARCHE_ID, USER_ID))
                .andExpect(status().isNoContent());

        verify(manageVoiceSessionUseCase).forceDisconnect(PARCHE_ID, USER_ID);
    }
}
