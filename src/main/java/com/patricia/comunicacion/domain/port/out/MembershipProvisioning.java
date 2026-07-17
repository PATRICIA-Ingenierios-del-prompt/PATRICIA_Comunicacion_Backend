package com.patricia.comunicacion.domain.port.out;

/**
 * Alta de membresía en la caché local (tabla parche_member). Complemento de
 * {@link MembershipVerification}: los parches se alimentan por eventos de
 * RabbitMQ, pero los canales DM no tienen parche detrás, así que el propio
 * servicio registra a los dos participantes al asegurar el canal.
 */
public interface MembershipProvisioning {

    /** Registra al usuario como miembro del canal. Idempotente. */
    void ensureMember(String channelId, String userId);
}
