package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import com.patricia.comunicacion.infrastructure.web.dto.ChatMessagePayload;
import com.patricia.comunicacion.infrastructure.web.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "Chat", description = "Historial de mensajes y moderación (RF-PAR-04)")
@SecurityRequirement(name = "Bearer")
public class ChatHistoryController {

    private final GetMessageHistoryUseCase getMessageHistoryUseCase;

    public ChatHistoryController(GetMessageHistoryUseCase getMessageHistoryUseCase) {
        this.getMessageHistoryUseCase = getMessageHistoryUseCase;
    }

    @GetMapping("/{parcheId}/messages")
    @Operation(summary = "Historial paginado de mensajes de un parche")
    public ResponseEntity<PageResponse<ChatMessagePayload>> getHistory(
            @PathVariable String parcheId,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication auth) {

        PageResponse<ChatMessagePayload> response = PageResponse.from(
                getMessageHistoryUseCase
                        .execute(parcheId, auth.getName(), pageable)
                        .map(ChatMessagePayload::fromDomain)
        );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{parcheId}/messages/{messageId}")
    @Operation(summary = "Eliminar mensaje (remitente o creador del parche)")
    public ResponseEntity<Void> deleteMessage(
            @PathVariable String parcheId,
            @PathVariable String messageId,
            Authentication auth) {

        getMessageHistoryUseCase.deleteMessage(parcheId, messageId, auth.getName());
        return ResponseEntity.noContent().build();
    }
}