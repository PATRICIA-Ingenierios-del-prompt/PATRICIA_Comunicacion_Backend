package com.patricia.comunicacion.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatHistoryController unit tests")
class ChatHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock private GetMessageHistoryUseCase getMessageHistoryUseCase;

    @InjectMocks private ChatHistoryController chatHistoryController;

    private final Authentication auth =
            new UsernamePasswordAuthenticationToken("u-1", null, List.of());

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(chatHistoryController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    @DisplayName("GET /api/chat/{parcheId}/messages — debería retornar historial paginado")
    void getHistory_shouldReturnPagedMessages() throws Exception {
        Message msg = Message.builder()
                .id("msg-1").parcheId("p-1").senderId("u-1")
                .senderUsername("karol").content("Hola").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        Page<Message> page = new PageImpl<>(List.of(msg));
        when(getMessageHistoryUseCase.execute(eq("p-1"), eq("u-1"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/chat/p-1/messages").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].messageId").value("msg-1"))
                .andExpect(jsonPath("$.content[0].senderUsername").value("karol"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("DELETE /api/chat/{parcheId}/messages/{messageId} — debería retornar 204")
    void deleteMessage_shouldReturn204() throws Exception {
        doNothing().when(getMessageHistoryUseCase)
                .deleteMessage(eq("p-1"), eq("msg-1"), eq("u-1"));

        mockMvc.perform(delete("/api/chat/p-1/messages/msg-1").principal(auth))
                .andExpect(status().isNoContent());

        verify(getMessageHistoryUseCase).deleteMessage("p-1", "msg-1", "u-1");
    }
}