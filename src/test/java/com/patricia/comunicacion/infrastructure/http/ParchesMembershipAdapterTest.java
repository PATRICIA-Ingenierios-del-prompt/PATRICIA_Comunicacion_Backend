package com.patricia.comunicacion.infrastructure.http;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.infrastructure.persistence.entity.ParcheMemberEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ParchesMembershipAdapter unit tests")
class ParchesMembershipAdapterTest {

    @Mock private ParcheMemberJpaRepository memberRepository;

    private ParchesMembershipAdapter adapter;

    private static final String PARCHE_ID = "parche-001";
    private static final String USER_ID   = "user-001";

    @BeforeEach
    void setUp() {
        // URL de Parches Core — apunta a localhost para tests (no se llama realmente)
        adapter = new ParchesMembershipAdapter(memberRepository, "http://localhost:8083");
    }

    @Test
    @DisplayName("verify debería pasar si el usuario es miembro")
    void verify_shouldPassWhenMember() {
        when(memberRepository.existsByParcheIdAndUserId(PARCHE_ID, USER_ID)).thenReturn(true);

        // No debe lanzar excepción
        adapter.verify(PARCHE_ID, USER_ID);

        verify(memberRepository).existsByParcheIdAndUserId(PARCHE_ID, USER_ID);
    }

    @Test
    @DisplayName("verify debería lanzar MembershipException si el usuario no es miembro")
    void verify_shouldThrowWhenNotMember() {
        when(memberRepository.existsByParcheIdAndUserId(PARCHE_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> adapter.verify(PARCHE_ID, USER_ID))
                .isInstanceOf(MembershipException.class);
    }

    @Test
    @DisplayName("isParchePrivate debería retornar true cuando Parches Core no está disponible")
    void isParchePrivate_shouldReturnTrueWhenParchesCoreUnavailable() {
        // Al apuntar a localhost:8083 sin servidor real, lanzará excepción de conexión
        // y el fallback debe retornar true
        boolean result = adapter.isParchePrivate(PARCHE_ID);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("ensureMember debería guardar la membresía si no existe")
    void ensureMember_shouldSaveWhenNotExists() {
        when(memberRepository.existsByParcheIdAndUserId(PARCHE_ID, USER_ID)).thenReturn(false);

        adapter.ensureMember(PARCHE_ID, USER_ID);

        ArgumentCaptor<ParcheMemberEntity> captor = ArgumentCaptor.forClass(ParcheMemberEntity.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getParcheId()).isEqualTo(PARCHE_ID);
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("ensureMember debería ser idempotente si la membresía ya existe")
    void ensureMember_shouldSkipWhenAlreadyExists() {
        when(memberRepository.existsByParcheIdAndUserId(PARCHE_ID, USER_ID)).thenReturn(true);

        adapter.ensureMember(PARCHE_ID, USER_ID);

        verify(memberRepository, never()).save(any());
    }

    @Test
    @DisplayName("ensureMember debería tolerar la carrera contra el índice único")
    void ensureMember_shouldSwallowDataIntegrityViolation() {
        when(memberRepository.existsByParcheIdAndUserId(PARCHE_ID, USER_ID)).thenReturn(false);
        when(memberRepository.save(any(ParcheMemberEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        // No debe propagar la excepción — otra request concurrente ya insertó la fila
        adapter.ensureMember(PARCHE_ID, USER_ID);
    }

    @Test
    @DisplayName("findMembers debería devolver los userIds del canal")
    void findMembers_shouldReturnChannelMembers() {
        when(memberRepository.findUserIdsByParcheId(PARCHE_ID)).thenReturn(Set.of("user-a", "user-b"));

        Set<String> result = adapter.findMembers(PARCHE_ID);

        assertThat(result).containsExactlyInAnyOrder("user-a", "user-b");
    }

    @Test
    @DisplayName("findMembers debería devolver un conjunto vacío si el repositorio devuelve null")
    void findMembers_shouldReturnEmptyWhenNull() {
        when(memberRepository.findUserIdsByParcheId(PARCHE_ID)).thenReturn(null);

        Set<String> result = adapter.findMembers(PARCHE_ID);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getChannelName debería devolver 'chat privado' cuando Parches Core no responde")
    void getChannelName_shouldReturnChatPrivadoWhenParchesCoreUnavailable() {
        String name = adapter.getChannelName(PARCHE_ID);

        assertThat(name).isEqualTo("chat privado");
    }

    @Test
    @DisplayName("getChannelName debería devolver el nombre del parche desde Parches Core")
    void getChannelName_shouldReturnNameFromParchesCore() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/parches/" + PARCHE_ID, exchange -> {
            String body = "{\"name\":\"Salsa Crew\",\"description\":\"\",\"category\":\"\",\"visibility\":\"PUBLIC\",\"status\":\"\",\"maxCapacity\":10,\"memberCount\":2}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.getBytes().length);
            exchange.getResponseBody().write(body.getBytes());
            exchange.close();
        });
        server.start();
        try {
            int port = server.getAddress().getPort();
            ParchesMembershipAdapter localAdapter = new ParchesMembershipAdapter(memberRepository, "http://localhost:" + port);

            String name = localAdapter.getChannelName(PARCHE_ID);

            assertThat(name).isEqualTo("Salsa Crew");
        } finally {
            server.stop(0);
        }
    }
}
