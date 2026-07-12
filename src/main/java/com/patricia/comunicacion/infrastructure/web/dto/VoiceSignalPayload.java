package com.patricia.comunicacion.infrastructure.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSignalPayload {

    public enum SignalType { OFFER, ANSWER, ICE_CANDIDATE, JOIN, LEAVE }

    private SignalType signalType;
    private String targetUserId;
    private String signalData;
    private String senderUserId;
    private String senderUsername;
}
