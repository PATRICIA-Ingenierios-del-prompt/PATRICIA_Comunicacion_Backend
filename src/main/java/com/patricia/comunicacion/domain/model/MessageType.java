package com.patricia.comunicacion.domain.model;

public enum MessageType {
    TEXT,
    IMAGE,
    /** Documento adjunto (PDF, Word, etc.) — usa fileUrl para la URL. */
    FILE,
    SYSTEM
}
