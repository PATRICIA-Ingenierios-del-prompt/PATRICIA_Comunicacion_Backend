package com.patricia.comunicacion.domain.port.out;

import com.patricia.comunicacion.domain.model.Message;

public interface MessageBroker {

    void publish(String parcheId, Message message);

    void publishForceDisconnect(String parcheId, String userId);
}
