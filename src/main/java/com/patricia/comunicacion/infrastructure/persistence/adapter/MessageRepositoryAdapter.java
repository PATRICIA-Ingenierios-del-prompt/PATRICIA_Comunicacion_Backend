package com.patricia.comunicacion.infrastructure.persistence.adapter;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import com.patricia.comunicacion.infrastructure.persistence.entity.MessageEntity;
import com.patricia.comunicacion.infrastructure.persistence.repository.MessageJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Component
public class MessageRepositoryAdapter implements MessageRepository {

    private final MessageJpaRepository jpaRepository;

    public MessageRepositoryAdapter(MessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public Message save(Message message) {
        return toDomain(jpaRepository.save(toEntity(message)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> findByParcheId(String parcheId, Pageable pageable) {
        // Crear Pageable sin sort — el repositorio ya ordena por sentAt DESC en el nombre del método
        Pageable cleanPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        return jpaRepository
                .findByParcheIdAndDeletedFalseOrderBySentAtDesc(parcheId, cleanPageable)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void markAllAsReadByUser(String parcheId, String userId) {
        jpaRepository.markAllAsRead(parcheId, userId);
    }

    @Override
    @Transactional
    public void softDelete(String messageId) {
        jpaRepository.findById(messageId).ifPresent(e -> {
            e.setDeleted(true);
            jpaRepository.save(e);
        });
    }

    private MessageEntity toEntity(Message m) {
        MessageEntity e = new MessageEntity();
        e.setId(m.getId());
        e.setParcheId(m.getParcheId());
        e.setSenderId(m.getSenderId());
        e.setSenderUsername(m.getSenderUsername());
        e.setContent(m.getContent());
        e.setFileUrl(m.getFileUrl());
        e.setType(m.getType());
        e.setSentAt(m.getSentAt());
        e.setReadBy(m.getReadBy() != null ? m.getReadBy() : new HashSet<>());
        e.setDeleted(m.isDeleted());
        return e;
    }

    private Message toDomain(MessageEntity e) {
        return Message.builder()
                .id(e.getId())
                .parcheId(e.getParcheId())
                .senderId(e.getSenderId())
                .senderUsername(e.getSenderUsername())
                .content(e.getContent())
                .fileUrl(e.getFileUrl())
                .type(e.getType())
                .sentAt(e.getSentAt())
                .readBy(e.getReadBy())
                .deleted(e.isDeleted())
                .build();
    }
}
