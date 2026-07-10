package com.patricia.comunicacion.infrastructure.config;

import com.patricia.comunicacion.infrastructure.security.StompAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración STOMP del microservicio de Comunicación.
 *
 * Endpoints expuestos:
 *   /ws       — con SockJS (fallback HTTP, útil para navegadores sin WS nativo)
 *   /ws-stomp — WebSocket nativo puro, para el gateway de AWS (ALB ws://)
 *
 * El gateway enruta ws://<host>/ws-stomp/* hacia las instancias de este servicio.
 * La autenticación JWT se verifica en el frame CONNECT via StompAuthInterceptor.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Con SockJS — para desarrollo local y clientes web que no soportan WS nativo
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // WebSocket nativo — para el gateway de AWS (ALB / API Gateway WebSocket)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker simple en memoria — reemplazar por RabbitMQ STOMP broker
        // si se necesita escalar horizontalmente (múltiples instancias)
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
