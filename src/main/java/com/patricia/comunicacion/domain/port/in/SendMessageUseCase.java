package com.patricia.comunicacion.domain.port.in;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.model.MessageType;

public interface SendMessageUseCase {

    /**
     * Envía un mensaje de texto o sistema (sin adjunto).
     */
    Message execute(String parcheId, String senderId, String senderUsername,
                    String content, MessageType type);

    /**
     * Envía un mensaje con archivo adjunto (FILE o IMAGE).
     * @param fileUrl URL pública del archivo ya subido al storage.
     */
    Message executeWithFile(String parcheId, String senderId, String senderUsername,
                            String content, MessageType type, String fileUrl);

    void markAsRead(String parcheId, String userId);
}
