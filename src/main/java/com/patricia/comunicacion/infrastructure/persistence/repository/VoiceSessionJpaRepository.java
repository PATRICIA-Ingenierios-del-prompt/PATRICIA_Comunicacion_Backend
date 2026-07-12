package com.patricia.comunicacion.infrastructure.persistence.repository;

import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.infrastructure.persistence.entity.VoiceSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoiceSessionJpaRepository extends JpaRepository<VoiceSessionEntity, String> {

    Optional<VoiceSessionEntity> findByParcheIdAndUserIdAndStatus(
            String parcheId, String userId, VoiceSessionStatus status);

    List<VoiceSessionEntity> findByParcheIdAndStatus(
            String parcheId, VoiceSessionStatus status);

    @Modifying
    @Query("""
        UPDATE VoiceSessionEntity v
        SET v.status = com.patricia.comunicacion.domain.model.VoiceSessionStatus.DISCONNECTED,
            v.leftAt = :leftAt
        WHERE v.parcheId = :parcheId
          AND v.userId   = :userId
          AND v.status   = com.patricia.comunicacion.domain.model.VoiceSessionStatus.ACTIVE
        """)
    void deactivate(@Param("parcheId") String parcheId,
                    @Param("userId")   String userId,
                    @Param("leftAt")   Instant leftAt);
}
