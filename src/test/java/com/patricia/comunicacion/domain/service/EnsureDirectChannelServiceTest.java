package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.port.out.MembershipProvisioning;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnsureDirectChannelService unit tests")
class EnsureDirectChannelServiceTest {

    @Mock private MembershipProvisioning membershipProvisioning;

    private EnsureDirectChannelService service;

    private static final String USER_A = "11111111-1111-1111-1111-111111111111";
    private static final String USER_B = "22222222-2222-2222-2222-222222222222";

    @BeforeEach
    void setUp() {
        service = new EnsureDirectChannelService(membershipProvisioning);
    }

    @Test
    @DisplayName("debería devolver el mismo channelId sin importar quién abre el chat")
    void execute_shouldBeDeterministicRegardlessOfOrder() {
        String channelAB = service.execute(USER_A, USER_B);
        String channelBA = service.execute(USER_B, USER_A);

        assertThat(channelAB).isEqualTo(channelBA);
    }

    @Test
    @DisplayName("debería devolver un UUID válido que cabe en VARCHAR(36)")
    void execute_shouldReturnValidUuid() {
        String channelId = service.execute(USER_A, USER_B);

        assertThat(channelId)
                .hasSize(36)
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("debería registrar a ambos usuarios como miembros del canal")
    void execute_shouldProvisionBothUsers() {
        String channelId = service.execute(USER_A, USER_B);

        verify(membershipProvisioning).ensureMember(channelId, USER_A);
        verify(membershipProvisioning).ensureMember(channelId, USER_B);
    }

    @Test
    @DisplayName("pares distintos deberían producir canales distintos")
    void execute_differentPairsShouldYieldDifferentChannels() {
        String otherUser = "33333333-3333-3333-3333-333333333333";

        String channelAB = service.execute(USER_A, USER_B);
        String channelAC = service.execute(USER_A, otherUser);

        assertThat(channelAB).isNotEqualTo(channelAC);
    }

    @Test
    @DisplayName("debería rechazar abrir un chat con uno mismo")
    void execute_shouldRejectSelfChat() {
        assertThatThrownBy(() -> service.execute(USER_A, USER_A))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(membershipProvisioning);
    }

    @Test
    @DisplayName("debería rechazar otherUserId nulo o en blanco")
    void execute_shouldRejectBlankOtherUser() {
        assertThatThrownBy(() -> service.execute(USER_A, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.execute(USER_A, "  "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(membershipProvisioning);
    }
}
