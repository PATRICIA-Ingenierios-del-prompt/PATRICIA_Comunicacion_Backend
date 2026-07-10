package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatHistoryController unit tests")
class ChatHistoryControllerTest {

    @Mock private GetMessageHistoryUseCase getMessageHistoryUseCase;

    private MockMvc mockMvc;

    private static final String PARCHE_ID = "parche-001";
    private static final String USER_ID   = "user-001";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ChatHistoryController(getMessageHistoryUseCase))
                .setCustomArgumentResolvers(
                        new org.springframework.data.web.PageableHandlerMethodArgumentResolver())
                .build();
    }

    private UsernamePasswordAuthenticationToken auth() {
        return new UsernamePasswordAuthenticationToken(USER_ID, null, List.of());
    }

    @Test
    @DisplayName("GET /api/chat/{parcheId}/messages debería retornar 200 con mensajes paginados")
    void getHistory_shouldReturn200WithMessages() throws Exception {
        Message msg = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("david").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        when(getMessageHistoryUseCase.execute(eq(PARCHE_ID), eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(msg)));

        mockMvc.perform(get("/api/chat/{parcheId}/messages", PARCHE_ID)
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value("msg-001"))
                .andExpect(jsonPath("$.content[0].content").value("Hola!"));

    }

    @Test
    @DisplayName("GET /api/chat/{parcheId}/messages debería retornar 200 con lista vacía")
    void getHistory_shouldReturn200WithEmptyList() throws Exception {
        when(getMessageHistoryUseCase.execute(eq(PARCHE_ID), eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/chat/{parcheId}/messages", PARCHE_ID)
                        .principal(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    @DisplayName("DELETE /api/chat/{parcheId}/messages/{messageId} debería retornar 204")
    void deleteMessage_shouldReturn204() throws Exception {
        doNothing().when(getMessageHistoryUseCase)
                .deleteMessage(PARCHE_ID, "msg-001", USER_ID);

        mockMvc.perform(delete("/api/chat/{parcheId}/messages/{messageId}", PARCHE_ID, "msg-001")
                        .principal(auth()))
                .andExpect(status().isNoContent());

        verify(getMessageHistoryUseCase).deleteMessage(PARCHE_ID, "msg-001", USER_ID);
    }
}
