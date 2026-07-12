package com.patricia.comunicacion.domain.port.out;

import com.patricia.comunicacion.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MessageRepository {

    Message save(Message message);

    Page<Message> findByParcheId(String parcheId, Pageable pageable);

    void markAllAsReadByUser(String parcheId, String userId);

    void softDelete(String messageId);
}
