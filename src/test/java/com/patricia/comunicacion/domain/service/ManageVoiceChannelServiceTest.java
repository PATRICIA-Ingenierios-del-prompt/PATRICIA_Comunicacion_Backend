package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.VoiceChannelException;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.VoiceSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageVoiceChannelService unit tests")
class ManageVoiceChannelServiceTest {

    @Mock private VoiceSessionRepository voiceSessionRepository;
    @Mock private MembershipVerification membershipVerification;
    @Mock private MessageBroker messageBroker;
    @Mock private EventPublisher eventPublisher;

    private ManageVoiceChannelService service;

    private static final String PARCHE_ID = "parche-priv-001";
    private static final String USER_ID   = "user-789";

    @BeforeEach
    void setUp() {
        service = new ManageVoiceChannelService(voiceSessionRepository,
                membershipVerification, messageBroker, eventPublisher);
    }

    @Test
    @DisplayName("joinVoiceChannel debería crear sesión y publicar callStarted si es el primero")
    void joinVoiceChannel_shouldPublishCallStartedWhenFirstParticipant() {
        VoiceSession saved = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("karol").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(membershipVerification.isParchePrivate(PARCHE_ID)).thenReturn(true);
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of());
        when(voiceSessionRepository.save(any())).thenReturn(saved);

        VoiceSession result = service.joinVoiceChannel(PARCHE_ID, USER_ID, "karol", "sig-001");

        assertThat(result.getStatus()).isEqualTo(VoiceSessionStatus.ACTIVE);
        verify(eventPublisher).publishVoiceCallStarted(eq(PARCHE_ID), eq(saved));
        verify(eventPublisher, never()).publishVoiceParticipantJoined(any(), any(), anyInt());
    }

    @Test
    @DisplayName("joinVoiceChannel debería publicar participantJoined si ya hay otros activos")
    void joinVoiceChannel_shouldPublishParticipantJoinedWhenOthersPresent() {
        VoiceSession existing = VoiceSession.builder()
                .id("vs-000").parcheId(PARCHE_ID).userId("other-user")
                .username("existing").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        VoiceSession saved = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("karol").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(membershipVerification.isParchePrivate(PARCHE_ID)).thenReturn(true);
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of(existing));
        when(voiceSessionRepository.save(any())).thenReturn(saved);

        service.joinVoiceChannel(PARCHE_ID, USER_ID, "karol", "sig-001");

        verify(eventPublisher).publishVoiceParticipantJoined(eq(PARCHE_ID), eq(saved), eq(2));
        verify(eventPublisher, never()).publishVoiceCallStarted(any(), any());
    }

    @Test
    @DisplayName("joinVoiceChannel debería lanzar VoiceChannelException en parche público")
    void joinVoiceChannel_shouldThrowWhenParcheIsPublic() {
        when(membershipVerification.isParchePrivate(PARCHE_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.joinVoiceChannel(PARCHE_ID, USER_ID, "karol", "sig-001"))
                .isInstanceOf(VoiceChannelException.class)
                .hasMessageContaining("privado");

        verifyNoInteractions(voiceSessionRepository);
    }

    @Test
    @DisplayName("leaveVoiceChannel debería publicar callEnded si era el último participante")
    void leaveVoiceChannel_shouldPublishCallEndedWhenLastParticipant() {
        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("karol").joinedAt(Instant.now().minusSeconds(60))
                .status(VoiceSessionStatus.ACTIVE).build();

        when(voiceSessionRepository.findActiveByParcheIdAndUserId(PARCHE_ID, USER_ID))
                .thenReturn(Optional.of(session));
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of());

        service.leaveVoiceChannel(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository).deactivate(PARCHE_ID, USER_ID);
        verify(eventPublisher).publishVoiceCallEnded(eq(PARCHE_ID), anyInt());
        verify(eventPublisher, never()).publishVoiceParticipantLeft(any(), any(), anyInt());
    }

    @Test
    @DisplayName("leaveVoiceChannel debería publicar participantLeft si quedan otros")
    void leaveVoiceChannel_shouldPublishParticipantLeftWhenOthersRemain() {
        VoiceSession session = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("karol").joinedAt(Instant.now().minusSeconds(30))
                .status(VoiceSessionStatus.ACTIVE).build();

        VoiceSession remaining = VoiceSession.builder()
                .id("vs-002").parcheId(PARCHE_ID).userId("other")
                .username("other").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(voiceSessionRepository.findActiveByParcheIdAndUserId(PARCHE_ID, USER_ID))
                .thenReturn(Optional.of(session));
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of(remaining));

        service.leaveVoiceChannel(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository).deactivate(PARCHE_ID, USER_ID);
        verify(eventPublisher).publishVoiceParticipantLeft(PARCHE_ID, USER_ID, 1);
        verify(eventPublisher, never()).publishVoiceCallEnded(any(), anyInt());
    }

    @Test
    @DisplayName("leaveVoiceChannel no debería hacer nada si la sesión no existe")
    void leaveVoiceChannel_shouldDoNothingWhenSessionNotFound() {
        when(voiceSessionRepository.findActiveByParcheIdAndUserId(PARCHE_ID, USER_ID))
                .thenReturn(Optional.empty());

        service.leaveVoiceChannel(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository, never()).deactivate(any(), any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("forceDisconnect debería desactivar sesión, publicar en Redis y callEnded si no quedan más")
    void forceDisconnect_shouldDeactivateAndPublishCallEndedWhenNoRemaining() {
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of());

        service.forceDisconnect(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository).deactivate(PARCHE_ID, USER_ID);
        verify(messageBroker).publishForceDisconnect(PARCHE_ID, USER_ID);
        verify(eventPublisher).publishVoiceCallEnded(PARCHE_ID, 0);
    }

    @Test
    @DisplayName("forceDisconnect debería publicar participantLeft si quedan otros")
    void forceDisconnect_shouldPublishParticipantLeftWhenOthersRemain() {
        VoiceSession remaining = VoiceSession.builder()
                .id("vs-002").parcheId(PARCHE_ID).userId("other")
                .username("other").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(List.of(remaining));

        service.forceDisconnect(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository).deactivate(PARCHE_ID, USER_ID);
        verify(messageBroker).publishForceDisconnect(PARCHE_ID, USER_ID);
        verify(eventPublisher).publishVoiceParticipantLeft(PARCHE_ID, USER_ID, 1);
    }

    @Test
    @DisplayName("getActiveParticipants debería delegar en el repositorio")
    void getActiveParticipants_shouldReturnFromRepository() {
        List<VoiceSession> sessions = List.of(
                VoiceSession.builder().userId("u1").status(VoiceSessionStatus.ACTIVE).build());
        when(voiceSessionRepository.findActiveByParcheId(PARCHE_ID)).thenReturn(sessions);

        List<VoiceSession> result = service.getActiveParticipants(PARCHE_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo("u1");
    }
}
