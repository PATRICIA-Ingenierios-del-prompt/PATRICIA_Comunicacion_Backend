package com.patricia.comunicacion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Caché local de membresía: quién pertenece a qué parche. Se alimenta por
 * eventos (parche.created agrega al dueño, parche.member.joined agrega al
 * que se une, parche.member.expelled lo retira) en vez de preguntarle a
 * Parches por HTTP en cada mensaje de chat o intento de unirse a voz.
 * <p>
 * Usa una PK autogenerada en vez de una clave compuesta (parcheId, userId)
 * para mantener el mapeo simple; la restricción real de unicidad vive en el
 * índice declarado en la migración de Flyway.
 */
@Entity
@Table(
    name = "parche_member",
    indexes = {
        @Index(name = "idx_parche_member_lookup", columnList = "parche_id, user_id", unique = true)
    }
)
@Getter
@Setter
public class ParcheMemberEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "parche_id", nullable = false, updatable = false)
    private String parcheId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private String userId;
}
