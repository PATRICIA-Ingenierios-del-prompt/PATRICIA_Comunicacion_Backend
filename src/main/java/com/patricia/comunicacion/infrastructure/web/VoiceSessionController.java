package com.patricia.comunicacion.infrastructure.web;

import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@Tag(name = "Canal de Voz", description = "Gestión de participantes (RF-PAR-05 - solo parches privados)")
@SecurityRequirement(name = "Bearer")
public class VoiceSessionController {

    private final ManageVoiceSessionUseCase manageVoiceSessionUseCase;

    public VoiceSessionController(ManageVoiceSessionUseCase manageVoiceSessionUseCase) {
        this.manageVoiceSessionUseCase = manageVoiceSessionUseCase;
    }

    @GetMapping("/{parcheId}/participants")
    @Operation(summary = "Participantes activos en el canal de voz")
    public ResponseEntity<List<Map<String, Object>>> getActiveParticipants(
            @PathVariable String parcheId) {

        List<Map<String, Object>> participants = manageVoiceSessionUseCase
                .getActiveParticipants(parcheId)
                .stream()
                .map(vs -> Map.<String, Object>of(
                        "userId",   vs.getUserId(),
                        "username", vs.getUsername(),
                        "joinedAt", vs.getJoinedAt().toString(),
                        "status",   vs.getStatus().name()
                ))
                .toList();

        return ResponseEntity.ok(participants);
    }

    @DeleteMapping("/{parcheId}/participants/{userId}")
    @Operation(summary = "Expulsar usuario del canal de voz (moderadores)")
    public ResponseEntity<Void> kickFromVoice(
            @PathVariable String parcheId,
            @PathVariable String userId) {

        manageVoiceSessionUseCase.forceDisconnect(parcheId, userId);
        return ResponseEntity.noContent().build();
    }
}
