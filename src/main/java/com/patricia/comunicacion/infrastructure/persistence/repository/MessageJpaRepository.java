package com.patricia.comunicacion.infrastructure.persistence.repository;

import com.patricia.comunicacion.infrastructure.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageJpaRepository extends JpaRepository<MessageEntity, String> {

    Page<MessageEntity> findByParcheIdAndDeletedFalseOrderBySentAtDesc(
            String parcheId, Pageable pageable);

    @Modifying
    @Query(value = """
        INSERT INTO message_read_by (message_id, user_id)
        SELECT m.id, :userId
        FROM messages m
        WHERE m.parche_id = :parcheId
          AND m.deleted   = false
          AND NOT EXISTS (
              SELECT 1 FROM message_read_by r
              WHERE r.message_id = m.id
                AND r.user_id    = :userId
          )
        """, nativeQuery = true)
    void markAllAsRead(@Param("parcheId") String parcheId,
                       @Param("userId")   String userId);
}
