package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.exception.MembershipException;
import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import com.patricia.comunicacion.domain.port.out.MembershipProvisioning;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SendMessageService unit tests")
class SendMessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private MembershipVerification membershipVerification;
    @Mock private MembershipProvisioning membershipProvisioning;
    @Mock private MessageBroker messageBroker;
    @Mock private EventPublisher eventPublisher;

    private SendMessageService service;

    private static final String PARCHE_ID = "parche-123";
    private static final String USER_ID   = "user-456";

    @BeforeEach
    void setUp() {
        service = new SendMessageService(messageRepository, membershipVerification,
                membershipProvisioning, messageBroker, eventPublisher);
    }

    @Test
    @DisplayName("Debería persistir, publicar en Redis y emitir evento de mensajería")
    void execute_shouldPersistBroadcastAndPublishEvent() {
        Message saved = Message.builder()
                .id("msg-789").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("juandc").content("Hola!").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(membershipProvisioning.findMembers(PARCHE_ID)).thenReturn(Set.of(USER_ID, "user-other"));
        when(membershipProvisioning.getChannelName(PARCHE_ID)).thenReturn("Parche test");
        when(messageRepository.save(any())).thenReturn(saved);

        Message result = service.execute(PARCHE_ID, USER_ID, "juandc", "Hola!", MessageType.TEXT);

        assertThat(result.getId()).isEqualTo("msg-789");
        verify(messageBroker).publish(PARCHE_ID, saved);
        verify(eventPublisher).publishMessageSent(saved, Set.of("user-other"), "Parche test");
    }

    @Test
    @DisplayName("Debería lanzar MembershipException si el usuario no es miembro")
    void execute_shouldThrowWhenNotMember() {
        doThrow(new MembershipException(USER_ID, PARCHE_ID))
                .when(membershipVerification).verify(PARCHE_ID, USER_ID);

        assertThatThrownBy(() ->
                service.execute(PARCHE_ID, USER_ID, "hacker", "intento", MessageType.TEXT))
                .isInstanceOf(MembershipException.class);

        verifyNoInteractions(messageRepository, messageBroker, eventPublisher);
    }

    @Test
    @DisplayName("Los mensajes SYSTEM no deben generar evento de mensajería")
    void execute_systemMessageShouldNotPublishEvent() {
        Message saved = Message.systemMessage(PARCHE_ID, "Juan se unió");
        doNothing().when(membershipVerification).verify(any(), any());
        when(messageRepository.save(any())).thenReturn(saved);

        service.execute(PARCHE_ID, "SYSTEM", "PATRICI.A", "Juan se unió", MessageType.SYSTEM);

        verify(eventPublisher, never()).publishMessageSent(any(), any(), any());
    }

    @Test
    @DisplayName("markAsRead debería delegar en el repositorio tras verificar membresía")
    void markAsRead_shouldVerifyAndDelegate() {
        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);

        service.markAsRead(PARCHE_ID, USER_ID);

        verify(messageRepository).markAllAsReadByUser(PARCHE_ID, USER_ID);
    }

    @Test
    @DisplayName("executeWithFile debería persistir mensaje con fileUrl")
    void executeWithFile_shouldPersistWithFileUrl() {
        Message saved = Message.builder()
                .id("msg-file-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("juandc").content("doc.pdf").type(MessageType.FILE)
                .fileUrl("https://storage.example.com/doc.pdf")
                .sentAt(Instant.now()).deleted(false).build();

        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(membershipProvisioning.findMembers(PARCHE_ID)).thenReturn(Set.of(USER_ID, "user-other"));
        when(membershipProvisioning.getChannelName(PARCHE_ID)).thenReturn("Parche test");
        when(messageRepository.save(any())).thenReturn(saved);

        Message result = service.executeWithFile(PARCHE_ID, USER_ID, "juandc",
                "doc.pdf", MessageType.FILE, "https://storage.example.com/doc.pdf");

        assertThat(result.getFileUrl()).isEqualTo("https://storage.example.com/doc.pdf");
        verify(eventPublisher).publishMessageSent(saved, Set.of("user-other"), "Parche test");
    }

    @Test
    @DisplayName("executeWithFile con tipo TEXT debe lanzar IllegalArgumentException")
    void executeWithFile_shouldThrowForTextType() {
        assertThatThrownBy(() ->
                service.executeWithFile(PARCHE_ID, USER_ID, "juandc",
                        "hola", MessageType.TEXT, "https://example.com/file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FILE o IMAGE");
    }

    @Test
    @DisplayName("executeWithFile con tipo IMAGE debería funcionar correctamente")
    void executeWithFile_shouldAcceptImageType() {
        Message saved = Message.builder()
                .id("msg-img-001").parcheId(PARCHE_ID).senderId(USER_ID)
                .senderUsername("juandc").content("foto.jpg").type(MessageType.IMAGE)
                .fileUrl("https://storage.example.com/foto.jpg")
                .sentAt(Instant.now()).deleted(false).build();

        doNothing().when(membershipVerification).verify(PARCHE_ID, USER_ID);
        when(membershipProvisioning.findMembers(PARCHE_ID)).thenReturn(Set.of(USER_ID, "user-other"));
        when(membershipProvisioning.getChannelName(PARCHE_ID)).thenReturn("Parche test");
        when(messageRepository.save(any())).thenReturn(saved);

        Message result = service.executeWithFile(PARCHE_ID, USER_ID, "juandc",
                "foto.jpg", MessageType.IMAGE, "https://storage.example.com/foto.jpg");

        assertThat(result.getType()).isEqualTo(MessageType.IMAGE);
        verify(eventPublisher).publishMessageSent(saved, Set.of("user-other"), "Parche test");
    }
}
