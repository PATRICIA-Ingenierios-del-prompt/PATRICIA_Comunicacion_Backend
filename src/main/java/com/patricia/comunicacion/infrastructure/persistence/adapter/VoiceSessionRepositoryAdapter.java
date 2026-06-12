package com.patricia.comunicacion.infrastructure.persistence.adapter;

import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.out.VoiceSessionRepository;
import com.patricia.comunicacion.infrastructure.persistence.entity.VoiceSessionEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.VoiceSessionJpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class VoiceSessionRepositoryAdapter implements VoiceSessionRepository {

    private final VoiceSessionJpaRepository jpaRepository;

    public VoiceSessionRepositoryAdapter(VoiceSessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public VoiceSession save(VoiceSession session) {
        return toDomain(jpaRepository.save(toEntity(session)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VoiceSession> findActiveByParcheIdAndUserId(String parcheId, String userId) {
        return jpaRepository
                .findByParcheIdAndUserIdAndStatus(parcheId, userId, VoiceSessionStatus.ACTIVE)
                .map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VoiceSession> findActiveByParcheId(String parcheId) {
        return jpaRepository
                .findByParcheIdAndStatus(parcheId, VoiceSessionStatus.ACTIVE)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deactivate(String parcheId, String userId) {
        jpaRepository.deactivate(parcheId, userId, Instant.now());
    }

    private VoiceSessionEntity toEntity(VoiceSession vs) {
        VoiceSessionEntity e = new VoiceSessionEntity();
        e.setId(vs.getId());
        e.setParcheId(vs.getParcheId());
        e.setUserId(vs.getUserId());
        e.setUsername(vs.getUsername());
        e.setSignalingSessionId(vs.getSignalingSessionId());
        e.setJoinedAt(vs.getJoinedAt());
        e.setLeftAt(vs.getLeftAt());
        e.setStatus(vs.getStatus());
        return e;
    }

    private VoiceSession toDomain(VoiceSessionEntity e) {
        return VoiceSession.builder()
                .id(e.getId())
                .parcheId(e.getParcheId())
                .userId(e.getUserId())
                .username(e.getUsername())
                .signalingSessionId(e.getSignalingSessionId())
                .joinedAt(e.getJoinedAt())
                .leftAt(e.getLeftAt())
                .status(e.getStatus())
                .build();
    }
}
