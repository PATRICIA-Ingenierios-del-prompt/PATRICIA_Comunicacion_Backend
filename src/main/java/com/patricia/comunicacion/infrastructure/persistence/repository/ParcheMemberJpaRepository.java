package com.patricia.comunicacion.infrastructure.persistence.repository;

import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface ParcheMemberJpaRepository extends JpaRepository<ParcheMemberEntity, String> {

    boolean existsByParcheIdAndUserId(String parcheId, String userId);

    @Query("SELECT m.userId FROM ParcheMemberEntity m WHERE m.parcheId = :parcheId")
    Set<String> findUserIdsByParcheId(@Param("parcheId") String parcheId);

    @Modifying
    @Query("DELETE FROM ParcheMemberEntity m WHERE m.parcheId = :parcheId AND m.userId = :userId")
    void deleteByParcheIdAndUserId(@Param("parcheId") String parcheId, @Param("userId") String userId);

    @Modifying
    @Query("DELETE FROM ParcheMemberEntity m WHERE m.parcheId = :parcheId")
    void deleteAllByParcheId(@Param("parcheId") String parcheId);
}
