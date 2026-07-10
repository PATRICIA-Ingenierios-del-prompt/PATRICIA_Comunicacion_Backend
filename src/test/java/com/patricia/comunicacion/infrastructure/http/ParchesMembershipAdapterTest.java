package com.patricia.comunicacion.infrastructure.http;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.infrastructure.persistence.repository.ParcheMemberJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
