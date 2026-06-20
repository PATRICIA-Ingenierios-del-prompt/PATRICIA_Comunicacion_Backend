package com.patricia.comunicacion.domain.port.in;

import com.patricia.comunicacion.domain.model.VoiceSession;

import java.util.List;

public interface ManageVoiceSessionUseCase {

    VoiceSession joinVoiceChannel(String parcheId, String userId,
                                   String username, String signalingSessionId);

    void leaveVoiceChannel(String parcheId, String userId);

    List<VoiceSession> getActiveParticipants(String parcheId);

    void forceDisconnect(String parcheId, String userId);
}
