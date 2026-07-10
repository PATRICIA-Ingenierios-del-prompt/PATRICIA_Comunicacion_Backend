package com.patricia.comunicacion.infrastructure.messaging;

import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import com.patricia.comunicacion.infrastructure.messaging.event.*;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheChannelEntity;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheMemberEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheChannelJpaRepository;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import com.patricia.comunicacion.infrastructure.ws.ComunicacionBroadcaster;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParcheLifecycleListener unit tests")
class ParcheLifecycleListenerTest {

    @Mock private ParcheChannelJpaRepository channelRepository;
    @Mock private ParcheMemberJpaRepository memberRepository;
    @Mock private ManageVoiceSessionUseCase manageVoiceSessionUseCase;
    @Mock private MessageRepository messageRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private ComunicacionBroadcaster broadcaster;

    private ParcheLifecycleListener listener;

    private static final UUID PARCHE_UUID = UUID.fromString("a3751732-044b-4a45-9829-438824557faf");
    private static final UUID OWNER_UUID  = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MEMBER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        listener = new ParcheLifecycleListener(
                channelRepository, memberRepository, manageVoiceSessionUseCase,
                messageRepository, messagingTemplate, rabbitTemplate, broadcaster);
    }

    // ── onParcheCreated ───────────────────────────────────────────────────────

    @Test
    @DisplayName("onParcheCreated debería provisionar canal nuevo y publicar CommunicationReadyEvent")
    void onParcheCreated_shouldProvisionNewChannelAndPublishReady() {
        ParcheCreatedEvent event = new ParcheCreatedEvent(
                UUID.randomUUID(), PARCHE_UUID, "Test Parche", "PUBLIC", OWNER_UUID);

        when(channelRepository.findById(PARCHE_UUID.toString())).thenReturn(Optional.empty());

        ParcheChannelEntity savedChannel = new ParcheChannelEntity();
        savedChannel.setParcheId(PARCHE_UUID.toString());
        savedChannel.setChatId(PARCHE_UUID.toString());
        savedChannel.setVoiceId(UUID.randomUUID().toString());
        savedChannel.setCreatedAt(Instant.now());
        when(channelRepository.save(any())).thenReturn(savedChannel);

        listener.onParcheCreated(event);

        verify(channelRepository).save(any(ParcheChannelEntity.class));
        verify(memberRepository).save(any(ParcheMemberEntity.class));
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CommunicationReadyEvent.class));
    }

    @Test
    @DisplayName("onParcheCreated debería reutilizar canal existente en reentrega")
    void onParcheCreated_shouldReuseExistingChannelOnRedelivery() {
        ParcheCreatedEvent event = new ParcheCreatedEvent(
                UUID.randomUUID(), PARCHE_UUID, "Test Parche", "PUBLIC", OWNER_UUID);

        ParcheChannelEntity existing = new ParcheChannelEntity();
        existing.setParcheId(PARCHE_UUID.toString());
        existing.setChatId(PARCHE_UUID.toString());
        existing.setVoiceId(UUID.randomUUID().toString());
        existing.setCreatedAt(Instant.now());

        when(channelRepository.findById(PARCHE_UUID.toString())).thenReturn(Optional.of(existing));

        listener.onParcheCreated(event);

        verify(channelRepository, never()).save(any());
        verify(memberRepository, never()).save(any());
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(CommunicationReadyEvent.class));
    }

    // ── onParcheMemberJoined ──────────────────────────────────────────────────

    @Test
    @DisplayName("onParcheMemberJoined debería registrar nuevo miembro")
    void onParcheMemberJoined_shouldRegisterNewMember() {
        ParcheMemberJoinedEvent event = new ParcheMemberJoinedEvent(PARCHE_UUID, MEMBER_UUID);
        when(memberRepository.existsByParcheIdAndUserId(
                PARCHE_UUID.toString(), MEMBER_UUID.toString())).thenReturn(false);

        listener.onParcheMemberJoined(event);

        verify(memberRepository).save(any(ParcheMemberEntity.class));
    }

    @Test
    @DisplayName("onParcheMemberJoined debería ignorar reentrega si ya existe el miembro")
    void onParcheMemberJoined_shouldIgnoreDuplicate() {
        ParcheMemberJoinedEvent event = new ParcheMemberJoinedEvent(PARCHE_UUID, MEMBER_UUID);
        when(memberRepository.existsByParcheIdAndUserId(
                PARCHE_UUID.toString(), MEMBER_UUID.toString())).thenReturn(true);

        listener.onParcheMemberJoined(event);

        verify(memberRepository, never()).save(any());
    }

    // ── onParcheDeleted ───────────────────────────────────────────────────────

    @Test
    @DisplayName("onParcheDeleted debería limpiar recursos y notificar clientes")
    void onParcheDeleted_shouldCleanResourcesAndNotify() {
        ParcheDeletedEvent event = new ParcheDeletedEvent(PARCHE_UUID, OWNER_UUID, Set.of());

        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_UUID.toString()).userId(OWNER_UUID.toString())
                .username("david").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(manageVoiceSessionUseCase.getActiveParticipants(PARCHE_UUID.toString()))
                .thenReturn(List.of(session));

        listener.onParcheDeleted(event);

        verify(manageVoiceSessionUseCase).forceDisconnect(PARCHE_UUID.toString(), OWNER_UUID.toString());
        verify(messagingTemplate).convertAndSendToUser(
                eq(OWNER_UUID.toString()), eq("/queue/kicked"), any());
        verify(memberRepository).deleteAllByParcheId(PARCHE_UUID.toString());
        verify(channelRepository).deleteById(PARCHE_UUID.toString());
        verify(broadcaster).broadcast(eq("/topic/chat/" + PARCHE_UUID), any());
    }

    @Test
    @DisplayName("onParcheDeleted sin participantes de voz debería solo limpiar datos")
    void onParcheDeleted_shouldCleanDataWhenNoVoiceParticipants() {
        ParcheDeletedEvent event = new ParcheDeletedEvent(PARCHE_UUID, OWNER_UUID, Set.of());
        when(manageVoiceSessionUseCase.getActiveParticipants(PARCHE_UUID.toString()))
                .thenReturn(List.of());

        listener.onParcheDeleted(event);

        verify(manageVoiceSessionUseCase, never()).forceDisconnect(any(), any());
        verify(memberRepository).deleteAllByParcheId(PARCHE_UUID.toString());
        verify(channelRepository).deleteById(PARCHE_UUID.toString());
        verify(broadcaster).broadcast(eq("/topic/chat/" + PARCHE_UUID), any());
    }

    // ── onParcheMemberExpelled ────────────────────────────────────────────────

    @Test
    @DisplayName("onParcheMemberExpelled debería eliminar miembro y notificar expulsión")
    void onParcheMemberExpelled_shouldRemoveMemberAndNotify() {
        ParcheMemberExpelledEvent event = new ParcheMemberExpelledEvent(PARCHE_UUID, MEMBER_UUID);

        listener.onParcheMemberExpelled(event);

        verify(memberRepository).deleteByParcheIdAndUserId(
                PARCHE_UUID.toString(), MEMBER_UUID.toString());
        verify(manageVoiceSessionUseCase).forceDisconnect(
                PARCHE_UUID.toString(), MEMBER_UUID.toString());
        verify(messagingTemplate).convertAndSendToUser(
                eq(MEMBER_UUID.toString()), eq("/queue/kicked"), any());
    }
}
