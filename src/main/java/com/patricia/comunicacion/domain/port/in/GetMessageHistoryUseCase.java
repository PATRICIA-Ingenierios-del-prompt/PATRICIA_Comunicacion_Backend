package com.patricia.comunicacion.domain.port.in;

import com.patricia.comunicacion.domain.model.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GetMessageHistoryUseCase {

    Page<Message> execute(String parcheId, String requestingUserId, Pageable pageable);

    void deleteMessage(String parcheId, String messageId, String requestingUserId);
}
