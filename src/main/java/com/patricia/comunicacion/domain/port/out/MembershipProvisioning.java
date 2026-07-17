package com.patricia.comunicacion.domain.port.out;

/**
 * Alta de membresía en la caché local (tabla parche_member). Complemento de
 * {@link MembershipVerification}: los parches se alimentan por eventos de
 * RabbitMQ, pero los canales DM no tienen parche detrás, así que el propio
 * servicio registra a los dos participantes al asegurar el canal.
 */
import java.util.Set;

public interface MembershipProvisioning {

    /** Registra al usuario como miembro del canal. Idempotente. */
    void ensureMember(String channelId, String userId);

    /** Devuelve los userIds miembros del canal (incluye al remitente). */
    Set<String> findMembers(String channelId);

    /** Devuelve un nombre descriptivo del canal (nombre del parche o "chat privado"). */
    String getChannelName(String channelId);
}
