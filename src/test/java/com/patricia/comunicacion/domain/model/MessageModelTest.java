package com.patricia.comunicacion.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain model unit tests")
class MessageModelTest {

    @Test
    @DisplayName("Message.systemMessage — crea mensaje con senderId SYSTEM")
    void systemMessage_setsSystemSender() {
        Message msg = Message.systemMessage("parche-1", "Juan se unió");

        assertEquals("SYSTEM",    msg.getSenderId());
        assertEquals("PATRICI.A", msg.getSenderUsername());
        assertEquals(MessageType.SYSTEM, msg.getType());
        assertEquals("parche-1",  msg.getParcheId());
        assertFalse(msg.isDeleted());
        assertNotNull(msg.getSentAt());
    }

    @Test
    @DisplayName("Message builder — construye todos los campos correctamente")
    void message_builderConstructsAllFields() {
        Instant now = Instant.now();
        Message msg = Message.builder()
                .id("id-1")
                .parcheId("p-1")
                .senderId("u-1")
                .senderUsername("karol")
                .content("Hola!")
                .type(MessageType.TEXT)
                .sentAt(now)
                .deleted(false)
                .build();

        assertEquals("id-1",  msg.getId());
        assertEquals("p-1",   msg.getParcheId());
        assertEquals("u-1",   msg.getSenderId());
        assertEquals("karol", msg.getSenderUsername());
        assertEquals("Hola!", msg.getContent());
        assertEquals(MessageType.TEXT, msg.getType());
        assertEquals(now, msg.getSentAt());
    }

    @Test
    @DisplayName("Message.withDeleted — crea copia inmutable con deleted=true")
    void message_withDeletedCreatesNewInstance() {
        Message original = Message.builder()
                .id("id-1").parcheId("p-1").senderId("u-1")
                .senderUsername("karol").content("msg").type(MessageType.TEXT)
                .sentAt(Instant.now()).deleted(false).build();

        Message deleted = original.withDeleted(true);

        assertFalse(original.isDeleted());
        assertTrue(deleted.isDeleted());
        assertEquals(original.getId(), deleted.getId());
    }

    @Test
    @DisplayName("VoiceSession.disconnect — cambia status a DISCONNECTED")
    void voiceSession_disconnectChangesStatus() {
        VoiceSession session = VoiceSession.builder()
                .id("vs-1").parcheId("p-1").userId("u-1").username("karol")
                .joinedAt(Instant.now()).status(VoiceSessionStatus.ACTIVE).build();

        VoiceSession disconnected = session.disconnect();

        assertEquals(VoiceSessionStatus.DISCONNECTED, disconnected.getStatus());
        assertNotNull(disconnected.getLeftAt());
        // Original inmutable
        assertEquals(VoiceSessionStatus.ACTIVE, session.getStatus());
    }

    @Test
    @DisplayName("MessageType — todos los valores existen")
    void messageType_allValues() {
        assertEquals(4, MessageType.values().length);
        assertEquals(MessageType.TEXT,   MessageType.valueOf("TEXT"));
        assertEquals(MessageType.IMAGE,  MessageType.valueOf("IMAGE"));
        assertEquals(MessageType.FILE,   MessageType.valueOf("FILE"));
        assertEquals(MessageType.SYSTEM, MessageType.valueOf("SYSTEM"));
    }

    @Test
    @DisplayName("VoiceSessionStatus — todos los valores existen")
    void voiceSessionStatus_allValues() {
        assertEquals(2, VoiceSessionStatus.values().length);
        assertEquals(VoiceSessionStatus.ACTIVE,       VoiceSessionStatus.valueOf("ACTIVE"));
        assertEquals(VoiceSessionStatus.DISCONNECTED, VoiceSessionStatus.valueOf("DISCONNECTED"));
    }
}
