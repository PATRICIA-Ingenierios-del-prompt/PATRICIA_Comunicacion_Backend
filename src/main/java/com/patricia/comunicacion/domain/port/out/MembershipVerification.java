package com.patricia.comunicacion.domain.port.out;

public interface MembershipVerification {

    void verify(String parcheId, String userId);

    boolean isParchePrivate(String parcheId);
}
