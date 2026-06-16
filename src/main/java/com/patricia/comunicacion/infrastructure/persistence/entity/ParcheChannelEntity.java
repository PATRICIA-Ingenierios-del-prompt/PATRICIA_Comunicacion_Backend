package com.patricia.comunicacion.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Registro de aprovisionamiento: guarda el chatId y voiceId generados para
 * un parche la primera vez que llega el evento parche.created. Sirve también
 * como guardia de idempotencia — si RabbitMQ reentrega el mismo mensaje
 * (entrega "at-least-once"), no se generan IDs distintos ni se le manda a
 * Parches una segunda respuesta con datos diferentes.
 */
@Entity
@Table(name = "parche_channel")
@Getter
@Setter
public class ParcheChannelEntity {

    @Id
    @Column(name = "parche_id", nullable = false, updatable = false)
    private String parcheId;

    @Column(name = "chat_id", nullable = false, updatable = false)
    private String chatId;

    @Column(name = "voice_id", nullable = false, updatable = false)
    private String voiceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
