package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.VoiceChannelException;
import com.patricia.comunicacion.domain.model.VoiceSession;
import com.patricia.comunicacion.domain.model.VoiceSessionStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageVoiceChannelService unit tests")
class ManageVoiceChannelServiceTest {

    @Mock private VoiceSessionRepository voiceSessionRepository;
    @Mock private MembershipVerification membershipVerification;
    @Mock private MessageBroker messageBroker;

    private ManageVoiceChannelService service;

    private static final String PARCHE_ID = "parche-priv-001";
    private static final String USER_ID   = "user-789";

    @BeforeEach
    void setUp() {
        service = new ManageVoiceChannelService(voiceSessionRepository,
                membershipVerification, messageBroker);
    }

    @Test
    @DisplayName("joinVoiceChannel debería crear sesión en parche privado con membresía válida")
    void joinVoiceChannel_shouldCreateSessionWhenPrivateAndMember() {
        VoiceSession saved = VoiceSession.builder()
                .id("vs-001").parcheId(PARCHE_ID).userId(USER_ID)
                .username("karol").joinedAt(Instant.now())
                .status(VoiceSessionStatus.ACTIVE).build();

        when(membershipVerification.isParchePrivate(PARCHE_ID)).thenReturn(true);
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(voiceSessionRepository.save(any())).thenReturn(saved);

        VoiceSession result = service.joinVoiceChannel(PARCHE_ID, USER_ID, "karol", "sig-001");

        assertThat(result.getStatus()).isEqualTo(VoiceSessionStatus.ACTIVE);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("joinVoiceChannel debería lanzar VoiceChannelException en parche público (RF-PAR-05)")
    void joinVoiceChannel_shouldThrowWhenParcheIsPublic() {
        when(membershipVerification.isParchePrivate(PARCHE_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                service.joinVoiceChannel(PARCHE_ID, USER_ID, "karol", "sig-001"))
                .isInstanceOf(VoiceChannelException.class)
                .hasMessageContaining("privado");

        verifyNoInteractions(voiceSessionRepository);
    }

    @Test
    @DisplayName("forceDisconnect debería desactivar sesión y publicar señal en Redis")
    void forceDisconnect_shouldDeactivateAndPublish() {
        service.forceDisconnect(PARCHE_ID, USER_ID);

        verify(voiceSessionRepository).deactivate(PARCHE_ID, USER_ID);
        verify(messageBroker).publishForceDisconnect(PARCHE_ID, USER_ID);
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
