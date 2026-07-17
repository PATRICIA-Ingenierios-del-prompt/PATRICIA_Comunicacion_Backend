package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.port.in.EnsureDirectChannelUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DmController unit tests")
class DmControllerTest {

    @Mock private EnsureDirectChannelUseCase ensureDirectChannelUseCase;

    private MockMvc mockMvc;

    private static final String USER_ID  = "user-001";
    private static final String OTHER_ID = "user-002";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new DmController(ensureDirectChannelUseCase))
                .build();
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    @Test
    @DisplayName("POST /api/dm/{otherUserId} debería retornar 200 con el channelId")
    void ensureChannel_shouldReturn200WithChannelId() throws Exception {
        when(ensureDirectChannelUseCase.execute(USER_ID, OTHER_ID))
                .thenReturn("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

        mockMvc.perform(post("/api/dm/{otherUserId}", OTHER_ID)
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channelId")
                        .value("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
    }

    @Test
    @DisplayName("POST /api/dm/{otherUserId} debería retornar 400 si la validación falla")
    void ensureChannel_shouldReturn400OnValidationError() throws Exception {
        when(ensureDirectChannelUseCase.execute(USER_ID, USER_ID))
                .thenThrow(new IllegalArgumentException("No puedes abrir un chat privado contigo mismo"));

        mockMvc.perform(post("/api/dm/{otherUserId}", USER_ID)
                        .principal(auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("No puedes abrir un chat privado contigo mismo"));
    }
}
