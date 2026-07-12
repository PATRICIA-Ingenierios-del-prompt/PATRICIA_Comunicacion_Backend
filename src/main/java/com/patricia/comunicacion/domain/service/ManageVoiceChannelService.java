package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.VoiceChannelException;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import com.patricia.comunicacion.domain.port.out.VoiceSessionRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ManageVoiceChannelService implements ManageVoiceSessionUseCase {

    private final VoiceSessionRepository voiceSessionRepository;
    private final MembershipVerification membershipVerification;
    private final MessageBroker messageBroker;
    private final EventPublisher eventPublisher;

    public ManageVoiceChannelService(VoiceSessionRepository voiceSessionRepository,
                                      MembershipVerification membershipVerification,
                                      MessageBroker messageBroker,
                                      EventPublisher eventPublisher) {
        this.voiceSessionRepository = voiceSessionRepository;
        this.membershipVerification = membershipVerification;
        this.messageBroker = messageBroker;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public VoiceSession joinVoiceChannel(String parcheId, String userId,
                                          String username, String signalingSessionId) {
        // RF-PAR-05: solo parches privados tienen canal de voz
        if (!membershipVerification.isParchePrivate(parcheId)) {
            throw VoiceChannelException.notPrivateParche(parcheId);
        }
        membershipVerification.verify(parcheId, userId);

        List<VoiceSession> activeBeforeJoin = voiceSessionRepository.findActiveByParcheId(parcheId);

        VoiceSession session = VoiceSession.builder()
                .parcheId(parcheId)
                .userId(userId)
                .username(username)
                .signalingSessionId(signalingSessionId)
                .joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE)
                .build();

        VoiceSession saved = voiceSessionRepository.save(session);

        int totalNow = activeBeforeJoin.size() + 1;

        if (activeBeforeJoin.isEmpty()) {
            // Primera persona — la llamada acaba de empezar
            eventPublisher.publishVoiceCallStarted(parcheId, saved);
        } else {
            // Ya había participantes — alguien más se unió
            eventPublisher.publishVoiceParticipantJoined(parcheId, saved, totalNow);
        }

        return saved;
    }

    @Override
    public void leaveVoiceChannel(String parcheId, String userId) {
        voiceSessionRepository.findActiveByParcheIdAndUserId(parcheId, userId)
                .ifPresent(session -> {
                    voiceSessionRepository.deactivate(parcheId, userId);

                    List<VoiceSession> remaining = voiceSessionRepository.findActiveByParcheId(parcheId);
                    int remainingCount = remaining.size();

                    if (remainingCount == 0) {
                        // Última persona — la llamada terminó
                        int durationSeconds = (int) ChronoUnit.SECONDS.between(
                                session.getJoinedAt(), Instant.now());
                        eventPublisher.publishVoiceCallEnded(parcheId, durationSeconds);
                    } else {
                        eventPublisher.publishVoiceParticipantLeft(parcheId, userId, remainingCount);
                    }
                });
    }

    @Override
    public List<VoiceSession> getActiveParticipants(String parcheId) {
        return voiceSessionRepository.findActiveByParcheId(parcheId);
    }

    @Override
    public void forceDisconnect(String parcheId, String userId) {
        voiceSessionRepository.deactivate(parcheId, userId);
        messageBroker.publishForceDisconnect(parcheId, userId);

        List<VoiceSession> remaining = voiceSessionRepository.findActiveByParcheId(parcheId);
        if (remaining.isEmpty()) {
            eventPublisher.publishVoiceCallEnded(parcheId, 0);
        } else {
            eventPublisher.publishVoiceParticipantLeft(parcheId, userId, remaining.size());
        }
    }
}
