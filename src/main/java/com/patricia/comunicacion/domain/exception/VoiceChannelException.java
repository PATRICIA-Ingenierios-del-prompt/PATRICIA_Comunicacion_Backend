package com.patricia.comunicacion.domain.exception;

public class VoiceChannelException extends RuntimeException {
    public VoiceChannelException(String message) {
        super(message);
    }

    public static VoiceChannelException notPrivateParche(String parcheId) {
        return new VoiceChannelException(
                "El parche %s no es privado. Solo los parches privados tienen canal de voz (RF-PAR-05)."
                        .formatted(parcheId));
    }
}
