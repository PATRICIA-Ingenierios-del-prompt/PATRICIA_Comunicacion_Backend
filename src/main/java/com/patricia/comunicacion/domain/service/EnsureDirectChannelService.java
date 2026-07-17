package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.port.in.EnsureDirectChannelUseCase;
import com.patricia.comunicacion.domain.port.out.MembershipProvisioning;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class EnsureDirectChannelService implements EnsureDirectChannelUseCase {

    private final MembershipProvisioning membershipProvisioning;

    public EnsureDirectChannelService(MembershipProvisioning membershipProvisioning) {
        this.membershipProvisioning = membershipProvisioning;
    }

    @Override
    public String execute(String requestingUserId, String otherUserId) {
        if (otherUserId == null || otherUserId.isBlank()) {
            throw new IllegalArgumentException("otherUserId es obligatorio");
        }
        if (requestingUserId.equals(otherUserId)) {
            throw new IllegalArgumentException("No puedes abrir un chat privado contigo mismo");
        }

        String channelId = deterministicChannelId(requestingUserId, otherUserId);

        // Ambos participantes quedan como "miembros" del canal, así el resto
        // de la API (SendMessageService, GetMessageHistoryService) los deja
        // pasar sin cambios.
        membershipProvisioning.ensureMember(channelId, requestingUserId);
        membershipProvisioning.ensureMember(channelId, otherUserId);

        return channelId;
    }

    /**
     * UUID v3 derivado del par ordenado de usuarios: mismo par → mismo canal,
     * sin importar quién lo abre primero. Cabe en VARCHAR(36) como parche_id.
     */
    private String deterministicChannelId(String a, String b) {
        String lo = a.compareTo(b) <= 0 ? a : b;
        String hi = a.compareTo(b) <= 0 ? b : a;
        return UUID.nameUUIDFromBytes(("dm:" + lo + ":" + hi)
                .getBytes(StandardCharsets.UTF_8)).toString();
    }
}
