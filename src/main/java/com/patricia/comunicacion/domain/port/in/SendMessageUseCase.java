package com.patricia.comunicacion.domain.port.in;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;

public interface SendMessageUseCase {

    Message execute(String parcheId, String senderId, String senderUsername,
                    String content, MessageType type);

    void markAsRead(String parcheId, String userId);
}
