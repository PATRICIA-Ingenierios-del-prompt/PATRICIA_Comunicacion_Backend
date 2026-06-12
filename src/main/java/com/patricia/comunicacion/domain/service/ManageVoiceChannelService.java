package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.VoiceChannelException;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import com.patricia.comunicacion.domain.port.out.VoiceSessionRepository;

import java.time.Instant;
import java.util.List;

public class ManageVoiceChannelService implements ManageVoiceSessionUseCase {

    private final VoiceSessionRepository voiceSessionRepository;
    private final MembershipVerification membershipVerification;
    private final MessageBroker messageBroker;

    public ManageVoiceChannelService(VoiceSessionRepository voiceSessionRepository,
                                      MembershipVerification membershipVerification,
                                      MessageBroker messageBroker) {
        this.voiceSessionRepository = voiceSessionRepository;
        this.membershipVerification = membershipVerification;
        this.messageBroker = messageBroker;
    }

    @Override
    public VoiceSession joinVoiceChannel(String parcheId, String userId,
                                          String username, String signalingSessionId) {
        // RF-PAR-05: solo parches privados tienen canal de voz
        if (!membershipVerification.isParchePrivate(parcheId)) {
            throw VoiceChannelException.notPrivateParche(parcheId);
        }
        membershipVerification.verify(parcheId, userId);

        VoiceSession session = VoiceSession.builder()
                .parcheId(parcheId)
                .userId(userId)
                .username(username)
                .signalingSessionId(signalingSessionId)
                .joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE)
                .build();

        return voiceSessionRepository.save(session);
    }

    @Override
    public void leaveVoiceChannel(String parcheId, String userId) {
        voiceSessionRepository.deactivate(parcheId, userId);
    }

    @Override
    public List<VoiceSession> getActiveParticipants(String parcheId) {
        return voiceSessionRepository.findActiveByParcheId(parcheId);
    }

    @Override
    public void forceDisconnect(String parcheId, String userId) {
        voiceSessionRepository.deactivate(parcheId, userId);
        messageBroker.publishForceDisconnect(parcheId, userId);
    }
}
