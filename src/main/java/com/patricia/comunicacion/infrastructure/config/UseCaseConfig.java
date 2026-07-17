package com.patricia.comunicacion.infrastructure.config;

import com.patricia.comunicacion.domain.port.in.EnsureDirectChannelUseCase;
import com.patricia.comunicacion.domain.port.in.GetMessageHistoryUseCase;
import com.patricia.comunicacion.domain.port.in.ManageVoiceSessionUseCase;
import com.patricia.comunicacion.domain.port.in.SendMessageUseCase;
import com.patricia.comunicacion.domain.port.out.*;
import com.patricia.comunicacion.domain.service.EnsureDirectChannelService;
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
            MembershipProvisioning membershipProvisioning,
            MessageBroker messageBroker,
            EventPublisher eventPublisher) {
        return new SendMessageService(messageRepository, membershipVerification,
                membershipProvisioning, messageBroker, eventPublisher);
    }

    @Bean
    public GetMessageHistoryUseCase getMessageHistoryUseCase(
            MessageRepository messageRepository,
            MembershipVerification membershipVerification) {
        return new GetMessageHistoryService(messageRepository, membershipVerification);
    }

    @Bean
    public EnsureDirectChannelUseCase ensureDirectChannelUseCase(
            MembershipProvisioning membershipProvisioning) {
        return new EnsureDirectChannelService(membershipProvisioning);
    }

    @Bean
    public ManageVoiceSessionUseCase manageVoiceSessionUseCase(
            com.patricia.comunicacion.domain.port.out.VoiceSessionRepository voiceSessionRepository,
            MembershipVerification membershipVerification,
            MessageBroker messageBroker,
            EventPublisher eventPublisher) {
        return new ManageVoiceChannelService(voiceSessionRepository, membershipVerification,
                messageBroker, eventPublisher);
    }
}
