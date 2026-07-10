package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetMessageHistoryService unit tests")
class GetMessageHistoryServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MembershipVerification membershipVerification;

    private GetMessageHistoryService service;

    private static final String PARCHE_ID = "parche-hist-001";
    private static final String USER_ID   = "user-hist-001";

    @BeforeEach
    void setUp() {
        service = new GetMessageHistoryService(messageRepository, membershipVerification);
    }

    @Test
    @DisplayName("execute debería retornar mensajes paginados para miembro válido")
    void execute_shouldReturnPagedMessagesForValidMember() {
        Pageable pageable = PageRequest.of(0, 20);
        Message msg = Message.builder()
                .id("msg-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("david").content("Hola").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        Page<Message> page = new PageImpl<>(List.of(msg));
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(messageRepository.findByParcheId(PARCHE_ID, pageable)).thenReturn(page);

        Page<Message> result = service.execute(PARCHE_ID, USER_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("msg-001");
        verify(membershipVerification).verify(PARCHE_ID, USER_ID);
    }

    @Test
    @DisplayName("execute debería lanzar MembershipException para no miembro")
    void execute_shouldThrowWhenNotMember() {
        Pageable pageable = PageRequest.of(0, 20);
        doThrow(new MembershipException(USER_ID, PARCHE_ID))
                .when(membershipVerification).verify(PARCHE_ID, USER_ID);

        assertThatThrownBy(() -> service.execute(PARCHE_ID, USER_ID, pageable))
                .isInstanceOf(MembershipException.class);

        verifyNoInteractions(messageRepository);
    }

    @Test
    @DisplayName("deleteMessage debería verificar membresía y hacer softDelete")
    void deleteMessage_shouldVerifyAndSoftDelete() {
        String messageId = "msg-del-001";
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);

        service.deleteMessage(PARCHE_ID, messageId, USER_ID);

        verify(membershipVerification).verify(PARCHE_ID, USER_ID);
        verify(messageRepository).softDelete(messageId);
    }

    @Test
    @DisplayName("deleteMessage debería lanzar MembershipException para no miembro")
    void deleteMessage_shouldThrowWhenNotMember() {
        String messageId = "msg-del-001";
        doThrow(new MembershipException(USER_ID, PARCHE_ID))
                .when(membershipVerification).verify(PARCHE_ID, USER_ID);

        assertThatThrownBy(() -> service.deleteMessage(PARCHE_ID, messageId, USER_ID))
                .isInstanceOf(MembershipException.class);

        verify(messageRepository, never()).softDelete(any());
    }
}
