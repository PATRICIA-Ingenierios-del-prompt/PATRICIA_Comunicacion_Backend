package com.patricia.comunicacion.domain.port.out;

import com.patricia.comunicacion.domain.model.VoiceSession;

import java.util.List;
import java.util.Optional;

public interface VoiceSessionRepository {

    VoiceSession save(VoiceSession session);

    Optional<VoiceSession> findActiveByParcheIdAndUserId(String parcheId, String userId);

    List<VoiceSession> findActiveByParcheId(String parcheId);

    void deactivate(String parcheId, String userId);
}
