package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;
import com.patricia.comunicacion.domain.port.in.SendMessageUseCase;
import com.patricia.comunicacion.domain.port.out.EventPublisher;
import com.patricia.comunicacion.domain.port.out.MessageBroker;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageRepository;

import java.time.Instant;

public class SendMessageService implements SendMessageUseCase {

    private final MessageRepository messageRepository;
    private final MembershipVerification membershipVerification;
    private final MessageBroker messageBroker;
    private final EventPublisher eventPublisher;

    public SendMessageService(MessageRepository messageRepository,
                               MembershipVerification membershipVerification,
                               MessageBroker messageBroker,
                               EventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.membershipVerification = membershipVerification;
        this.messageBroker = messageBroker;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Message execute(String parcheId, String senderId, String senderUsername,
                           String content, MessageType type) {
        membershipVerification.verify(parcheId, senderId);

        Message message = Message.builder()
                .parcheId(parcheId)
                .senderId(senderId)
                .senderUsername(senderUsername)
                .content(content)
                .type(type)
                .sentAt(Instant.now())
                .deleted(false)
                .build();

        Message saved = messageRepository.save(message);

        // Sync con otras instancias via Redis Pub/Sub
        messageBroker.publish(parcheId, saved);

        // Notificar a Notificaciones (solo mensajes de usuario)
        if (type != MessageType.SYSTEM) {
            eventPublisher.publishChatPendiente(saved);
        }

        return saved;
    }

    @Override
    public void markAsRead(String parcheId, String userId) {
        membershipVerification.verify(parcheId, userId);
        messageRepository.markAllAsReadByUser(parcheId, userId);
    }
}
