package com.patricia.comunicacion.domain.service;

import com.patricia.comunicacion.domain.model.Message;
import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import com.patricia.comunicacion.domain.port.out.MembershipVerification;
import com.patricia.comunicacion.domain.port.out.MessageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public class GetMessageHistoryService implements GetMessageHistoryUseCase {

    private final MessageRepository messageRepository;
    private final MembershipVerification membershipVerification;

    public GetMessageHistoryService(MessageRepository messageRepository,
                                     MembershipVerification membershipVerification) {
        this.messageRepository = messageRepository;
        this.membershipVerification = membershipVerification;
    }

    @Override
    public Page<Message> execute(String parcheId, String requestingUserId, Pageable pageable) {
        membershipVerification.verify(parcheId, requestingUserId);
        return messageRepository.findByParcheId(parcheId, pageable);
    }

    @Override
    public void deleteMessage(String parcheId, String messageId, String requestingUserId) {
        membershipVerification.verify(parcheId, requestingUserId);
        messageRepository.softDelete(messageId);
    }
}
