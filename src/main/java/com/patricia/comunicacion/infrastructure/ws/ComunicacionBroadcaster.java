package com.patricia.comunicacion.infrastructure.ws;

import com.patricia.comunicacion.infrastructure.backplane.RedisBackplanePublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Broadcaster STOMP con soporte de backplane Redis.
 *
 * Centraliza todos los broadcasts de Comunicación:
 *   - Si el backplane está activo (K8s, múltiples pods): publica en Redis.
 *     Todos los pods recibirán el mensaje por BackplaneStompRelay.
 *   - Si el backplane no está activo (dev local, un solo pod): emite localmente.
 *   - Si el publish a Redis falla: fallback a emisión local (al menos los
 *     clientes del propio pod ven el mensaje).
 *
 * SOLO para broadcasts (/topic/**). Los mensajes /user/queue/** son siempre
 * locales y se envían directamente con messagingTemplate.
 *
 * Uso — reemplaza:
 *   messagingTemplate.convertAndSend("/topic/chat/" + parcheId, payload);
 * Por:
 *   broadcaster.broadcast("/topic/chat/" + parcheId, payload);
 *
 * Para mensajes dirigidos a un usuario (antes: convertAndSendToUser), usar
 * sendToUser(...) en vez de llamar a messagingTemplate directamente -- así
 * también cruzan pods cuando el backplane está activo (necesario para
 * señalización WebRTC punto a punto: offer/answer/ICE candidates).
 */
@Slf4j
@Component
public class ComunicacionBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectProvider<RedisBackplanePublisher> backplane;

    public ComunicacionBroadcaster(SimpMessagingTemplate messagingTemplate,
                                   ObjectProvider<RedisBackplanePublisher> backplane) {
        this.messagingTemplate = messagingTemplate;
        this.backplane         = backplane;
    }

    /**
     * Emite un broadcast STOMP a todos los clientes suscritos al destination,
     * cruzando pods si el backplane está activo.
     *
     * @param destination destino STOMP ("/topic/chat/{parcheId}", "/topic/voice/{parcheId}", etc.)
     * @param payload     body del mensaje
     */
    public void broadcast(String destination, Object payload) {
        RedisBackplanePublisher publisher = backplane.getIfAvailable();
        if (publisher != null) {
            try {
                publisher.publish(destination, payload);
                return;
            } catch (RuntimeException ex) {
                log.warn("Backplane publish failed for {} — falling back to local broadcast: {}",
                        destination, ex.getMessage());
            }
        }
        // Sin backplane o con fallo: emitir localmente
        localBroadcast(destination, payload);
    }

    /**
     * Envía un mensaje dirigido a un usuario específico, cruzando pods si el
     * backplane está activo (necesario para señalización WebRTC: sin esto,
     * el offer/answer/ICE solo llega si emisor y destinatario terminan en
     * el mismo pod, lo que con {@code minReplicas > 1} no está garantizado
     * y falla en silencio -- cada usuario queda esperando participantes sin
     * ningún error visible).
     *
     * @param targetUserId  id del usuario destino (claim "sub" del JWT)
     * @param destination   destino relativo, ej. "/queue/voice-signal"
     * @param payload       body del mensaje
     */
    public void sendToUser(String targetUserId, String destination, Object payload) {
        RedisBackplanePublisher publisher = backplane.getIfAvailable();
        if (publisher != null) {
            try {
                publisher.publishToUser(targetUserId, destination, payload);
                return;
            } catch (RuntimeException ex) {
                log.warn("Backplane publishToUser failed for user {} — falling back to local send: {}",
                        targetUserId, ex.getMessage());
            }
        }
        // Sin backplane o con fallo: enviar solo localmente (mismo pod)
        try {
            messagingTemplate.convertAndSendToUser(targetUserId, destination, payload);
        } catch (RuntimeException ex) {
            log.warn("Local sendToUser also failed for user {}: {}", targetUserId, ex.getMessage());
        }
    }

    private void localBroadcast(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (RuntimeException ex) {
            log.warn("Local broadcast also failed for {}: {}", destination, ex.getMessage());
        }
    }
}
