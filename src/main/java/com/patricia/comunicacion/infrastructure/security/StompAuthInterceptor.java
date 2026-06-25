package com.patricia.comunicacion.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Interceptor STOMP que verifica el JWT en el frame CONNECT.
 *
 * El gateway (AWS ALB / API Gateway) enruta ws/* hacia esta instancia.
 * Este interceptor extrae userId y username del token y los persiste en
 * la sesión STOMP para que los controllers los lean con
 * SimpMessageHeaderAccessor.getSessionAttributes().
 *
 * Si el token es inválido o ausente, retorna null — STOMP descarta la
 * conexión sin emitir ninguna respuesta al cliente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtFilter jwtFilter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Solo validar en el frame inicial CONNECT
        if (!StompCommand.CONNECT.equals(accessor.getCommand())) return message;

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WS CONNECT rechazado: header Authorization ausente o malformado");
            return null;
        }

        String token = authHeader.substring(7);

        try {
            var claims = jwtFilter.parseClaims(token);

            String userId   = claims.getSubject();
            String username = claims.get("username", String.class);

            if (userId == null || userId.isBlank()) {
                log.warn("WS CONNECT rechazado: claim 'sub' vacío en el token");
                return null;
            }

            Objects.requireNonNull(accessor.getSessionAttributes()).put("userId",   userId);
            accessor.getSessionAttributes().put("username", username != null ? username : userId);

            // Registrar el usuario en el contexto STOMP para /user/queue/** destinations
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId, null, List.of()));

            log.debug("WS CONNECT autenticado [userId={}, username={}]", userId, username);
            return message;

        } catch (Exception e) {
            log.warn("WS CONNECT rechazado: token inválido — {}", e.getMessage());
            return null;
        }
    }
}
