package com.patricia.comunicacion.infrastructure.messaging;

import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import com.patricia.comunicacion.infrastructure.config.RabbitMQConfig;
import com.patricia.comunicacion.infrastructure.messaging.event.CommunicationReadyEvent;
import com.patricia.comunicacion.infrastructure.messaging.event.ParcheCreatedEvent;
import com.patricia.comunicacion.infrastructure.messaging.event.ParcheDeletedEvent;
import com.patricia.comunicacion.infrastructure.messaging.event.ParcheMemberExpelledEvent;
import com.patricia.comunicacion.infrastructure.messaging.event.ParcheMemberJoinedEvent;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheChannelEntity;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheMemberEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheChannelJpaRepository;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import com.patricia.comunicacion.infrastructure.ws.ComunicacionBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Escucha el ciclo de vida de un parche desde Parches Core y mantiene a
 * Comunicación sincronizada:
 * <p>
 * - parche.created   → aprovisiona chat + voz, registra al dueño como primer
 *                       miembro local y responde con CommunicationReadyEvent.
 * - parche.member.joined   → agrega el miembro a la caché local de membresía.
 * - parche.member.expelled → lo retira de la caché y lo desconecta del canal
 *                             de voz si estaba activo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ParcheLifecycleListener {

    private final ParcheChannelJpaRepository channelRepository;
    private final ParcheMemberJpaRepository memberRepository;
    private final ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RabbitTemplate rabbitTemplate;
    private final ComunicacionBroadcaster broadcaster;
    private static final String PARCHE_ID_KEY = "parcheId";


    @RabbitListener(queues = RabbitMQConfig.PARCHE_CREATED_QUEUE)
    @Transactional
    public void onParcheCreated(ParcheCreatedEvent event) {
        String parcheId = event.getParcheId().toString();
        String ownerId  = event.getOwnerId().toString();

        log.info("parche.created recibido [parcheId={}, ownerId={}]", parcheId, ownerId);

        ParcheChannelEntity channel = channelRepository.findById(parcheId).orElse(null);

        if (channel == null) {
            // Primera vez que vemos este parche: aprovisionamos.
            channel = new ParcheChannelEntity();
            channel.setParcheId(parcheId);
            channel.setChatId(parcheId);              // 1 parche = 1 chat, mismo id, sin indirección extra
            channel.setVoiceId(UUID.randomUUID().toString());
            channel.setCreatedAt(Instant.now());
            channelRepository.save(channel);

            ParcheMemberEntity ownerMember = new ParcheMemberEntity();
            ownerMember.setParcheId(parcheId);
            ownerMember.setUserId(ownerId);
            memberRepository.save(ownerMember);
        } else {
            // Mensaje reentregado por RabbitMQ (entrega at-least-once):
            // no regeneramos IDs, solo reenviamos los que ya existen.
            log.info("parche.created reentregado, reutilizando canal existente [parcheId={}]", parcheId);
        }

        CommunicationReadyEvent reply = new CommunicationReadyEvent(
                event.getParcheId(),
                UUID.fromString(channel.getChatId()),
                UUID.fromString(channel.getVoiceId()));

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PARCHE_EXCHANGE,
                RabbitMQConfig.COMMUNICATION_READY_ROUTING_KEY,
                reply);

        log.info("parche.communication.ready publicado [parcheId={}, chatId={}, voiceId={}]",
                parcheId, channel.getChatId(), channel.getVoiceId());
    }

    @RabbitListener(queues = RabbitMQConfig.PARCHE_MEMBER_JOINED_QUEUE)
    @Transactional
    public void onParcheMemberJoined(ParcheMemberJoinedEvent event) {
        String parcheId = event.getParcheId().toString();
        String memberId = event.getMemberId().toString();

        if (memberRepository.existsByParcheIdAndUserId(parcheId, memberId)) {
            return; // ya lo teníamos registrado, evita duplicados en reentregas
        }

        ParcheMemberEntity member = new ParcheMemberEntity();
        member.setParcheId(parcheId);
        member.setUserId(memberId);
        memberRepository.save(member);

        log.info("parche.member.joined procesado [parcheId={}, memberId={}]", parcheId, memberId);
    }

    /**
     * El parche fue borrado — limpiamos todo lo que le pertenecía en Comunicación:
     * 1. Desconectamos a todos los participantes activos en voz.
     * 2. Borramos la caché de membresía local.
     * 3. Borramos el registro de canales (chatId/voiceId).
     * 4. Avisamos a todos los clientes WebSocket conectados.
     *
     * El historial de mensajes (tabla messages) se conserva por trazabilidad —
     * simplemente ya no será accesible porque el parche dejó de existir.
     */
    @RabbitListener(queues = RabbitMQConfig.PARCHE_DELETED_QUEUE)
    @Transactional
    public void onParcheDeleted(ParcheDeletedEvent event) {
        String parcheId = event.getParcheId().toString();
        log.warn("parche.deleted recibido — limpiando recursos [parcheId={}]", parcheId);

        // 1. Forzar desconexión de todos los participantes activos en voz
        manageVoiceSessionUseCase.getActiveParticipants(parcheId)
                .forEach(session -> {
                    manageVoiceSessionUseCase.forceDisconnect(parcheId, session.getUserId());
                    messagingTemplate.convertAndSendToUser(
                            session.getUserId(),
                            "/queue/kicked",
                            Map.of(PARCHE_ID_KEY, parcheId, "reason", "El parche fue eliminado")
                    );
                });

        // 2. Limpiar membresías locales
        memberRepository.deleteAllByParcheId(parcheId);

        // 3. Limpiar registro de canales
        channelRepository.deleteById(parcheId);

        // 4. Notificar al canal de chat que fue cerrado
        broadcaster.broadcast(
                "/topic/chat/" + parcheId,
                Map.of("type", "PARCHE_DELETED", PARCHE_ID_KEY, parcheId));

        log.warn("Recursos de comunicación eliminados [parcheId={}]", parcheId);
    }

    @RabbitListener(queues = RabbitMQConfig.PARCHE_MEMBER_EXPELLED_QUEUE)
    @Transactional
    public void onParcheMemberExpelled(ParcheMemberExpelledEvent event) {
        String parcheId = event.getParcheId().toString();
        String memberId = event.getMemberId().toString();

        log.warn("parche.member.expelled recibido [parcheId={}, memberId={}]", parcheId, memberId);

        memberRepository.deleteByParcheIdAndUserId(parcheId, memberId);
        manageVoiceSessionUseCase.forceDisconnect(parcheId, memberId);

        messagingTemplate.convertAndSendToUser(
                memberId,
                "/queue/kicked",
                Map.of(PARCHE_ID_KEY, parcheId, "reason", "Fuiste expulsado del parche"));
    }
}
