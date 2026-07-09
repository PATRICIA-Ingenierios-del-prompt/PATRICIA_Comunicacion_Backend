package com.patricia.comunicacion.infrastructure.backplane.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patricia.comunicacion.infrastructure.backplane.BackplaneStompRelay;
import com.patricia.comunicacion.infrastructure.backplane.RedisBackplanePublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Backplane de Redis para escala horizontal.
 *
 * Solo activo cuando backplane.enabled=true (variable BACKPLANE_ENABLED en K8s).
 * En dev local, enabled=false y el broker STOMP simple funciona sin backplane.
 *
 * TRAMPA DE SPRING BOOT: declarar cualquier RedisConnectionFactory propio apaga
 * los beans auto-configurados. Por eso declaramos TAMBIÉN el factory del Redis
 * de cache (spring.data.redis.*) y lo marcamos @Primary para que RedisChatSubscriber
 * y demás adapters de Comunicación sigan apuntando al Redis correcto.
 */
@Configuration
@ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
public class BackplaneConfig {

    // ── Redis de cache/estado (spring.data.redis.*) — @Primary ───────────────

    @Bean
    @Primary
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.host}") String host,
            @Value("${spring.data.redis.port}") int port,
            @Value("${spring.data.redis.password:}") String password) {
        return connectionFactory(host, port, password);
    }

    @Bean
    @Primary
    public StringRedisTemplate stringRedisTemplate(
            @Qualifier("redisConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    // ── Redis del backplane (backplane.redis.*) ───────────────────────────────

    @Bean
    public LettuceConnectionFactory backplaneConnectionFactory(BackplaneProperties props) {
        return connectionFactory(
                props.getRedis().getHost(),
                props.getRedis().getPort(),
                props.getRedis().getPassword());
    }

    @Bean
    public StringRedisTemplate backplaneRedisTemplate(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    // ── Beans del backplane ───────────────────────────────────────────────────

    @Bean
    public RedisBackplanePublisher redisBackplanePublisher(
            @Qualifier("backplaneRedisTemplate") StringRedisTemplate backplaneRedisTemplate,
            ObjectMapper objectMapper,
            BackplaneProperties props) {
        return new RedisBackplanePublisher(backplaneRedisTemplate, objectMapper, props.getChannel());
    }

    @Bean
    public BackplaneStompRelay backplaneStompRelay(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        return new BackplaneStompRelay(messagingTemplate, objectMapper);
    }

    @Bean
    public RedisMessageListenerContainer backplaneListenerContainer(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory,
            BackplaneStompRelay relay,
            BackplaneProperties props) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(relay, new ChannelTopic(props.getChannel()));
        return container;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private LettuceConnectionFactory connectionFactory(String host, int port, String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        return new LettuceConnectionFactory(config);
    }
}
