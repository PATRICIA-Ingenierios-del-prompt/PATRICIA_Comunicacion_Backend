package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.port.in.EnsureDirectChannelUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Chats privados 1 a 1. Contrato esperado por el frontend
 * (src/services/dmService.ts): POST /api/dm/{otherUserId} → { channelId }.
 * El channelId devuelto se usa después como un parcheId normal en
 * /api/chat/{channelId}/messages y en los destinos STOMP.
 */
@RestController
@RequestMapping("/api/dm")
@Tag(name = "DM", description = "Chats privados 1 a 1")
@SecurityRequirement(name = "Bearer")
public class DmController {

    private final EnsureDirectChannelUseCase ensureDirectChannelUseCase;

    public DmController(EnsureDirectChannelUseCase ensureDirectChannelUseCase) {
        this.ensureDirectChannelUseCase = ensureDirectChannelUseCase;
    }

    @PostMapping("/{otherUserId}")
    @Operation(summary = "Obtiene (o crea) el canal de chat privado con otro usuario")
    public ResponseEntity<DmChannelResponse> ensureChannel(
            @PathVariable String otherUserId,
            Authentication auth) {

        String channelId = ensureDirectChannelUseCase.execute(auth.getName(), otherUserId);
        return ResponseEntity.ok(new DmChannelResponse(channelId));
    }

    public record DmChannelResponse(String channelId) {}

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
}
