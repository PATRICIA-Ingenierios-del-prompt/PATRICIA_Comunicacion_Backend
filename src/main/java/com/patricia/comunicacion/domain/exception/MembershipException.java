package com.patricia.comunicacion.domain.exception;

public class MembershipException extends RuntimeException {
    public MembershipException(String userId, String parcheId) {
        super("El usuario %s no es miembro del parche %s".formatted(userId, parcheId));
    }
}
