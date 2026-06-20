package com.patricia.comunicacion.domain.port.out;

import com.patricia.comunicacion.domain.model.Message;

public interface EventPublisher {

    void publishChatPendiente(Message message);
}
