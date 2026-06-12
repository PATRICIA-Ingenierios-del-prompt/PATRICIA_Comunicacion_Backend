package com.patricia.comunicacion.infrastructure.config;

import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.in.SendMessageUseCase;
import com.patricia.comunicacion.domain.port.out.*;
import com.patricia.comunicacion.domain.service.GetMessageHistoryService;
import com.patricia.comunicacion.domain.service.ManageVoiceChannelService;
import com.patricia.comunicacion.domain.service.SendMessageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public SendMessageUseCase sendMessageUseCase(
            MessageRepository messageRepository,
            MembershipVerification membershipVerification,
            MessageBroker messageBroker,
            EventPublisher eventPublisher) {
        return new SendMessageService(messageRepository, membershipVerification,
                messageBroker, eventPublisher);
    }

    @Bean
    public GetMessageHistoryUseCase getMessageHistoryUseCase(
            MessageRepository messageRepository,
            MembershipVerification membershipVerification) {
        return new GetMessageHistoryService(messageRepository, membershipVerification);
    }

    @Bean
    public ManageVoiceSessionUseCase manageVoiceSessionUseCase(
            com.patricia.comunicacion.domain.port.out.VoiceSessionRepository voiceSessionRepository,
            MembershipVerification membershipVerification,
            MessageBroker messageBroker) {
        return new ManageVoiceChannelService(voiceSessionRepository, membershipVerification, messageBroker);
    }
}
