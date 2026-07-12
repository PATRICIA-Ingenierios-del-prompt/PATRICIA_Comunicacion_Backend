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
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Backplane de Redis para escala horizontal.
 *
 * El factory del Redis de cache (spring.data.redis.*) siempre se crea, para
 * que la app pueda correr en una region sin Cluster #2 (backplane.enabled=false)
 * pero manteniendo TLS en el Redis principal. Los beans de pub/sub solo se crean
 * cuando backplane.enabled=true.
 *
 * TRAMPA DE SPRING BOOT: declarar cualquier RedisConnectionFactory propio apaga
 * los beans auto-configurados. Por eso este factory con TLS reemplaza al de
 * autoconfig -- que por default va sin SSL y falla el handshake contra
 * ElastiCache (ambos clusters tienen transit_encryption en Ulink_Infra).
 */
@Configuration
public class BackplaneConfig {

    // ── Redis de cache/estado (spring.data.redis.*) — siempre activo ────────

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

    // ── Beans exclusivos del backplane (backplane.redis.*) ──────────────────

    @Bean
    @ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
    public LettuceConnectionFactory backplaneConnectionFactory(BackplaneProperties props) {
        return connectionFactory(
                props.getRedis().getHost(),
                props.getRedis().getPort(),
                props.getRedis().getPassword());
    }

    @Bean
    @ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
    public StringRedisTemplate backplaneRedisTemplate(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    @ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
    public RedisBackplanePublisher redisBackplanePublisher(
            @Qualifier("backplaneRedisTemplate") StringRedisTemplate backplaneRedisTemplate,
            ObjectMapper objectMapper,
            BackplaneProperties props) {
        return new RedisBackplanePublisher(backplaneRedisTemplate, objectMapper, props.getChannel());
    }

    @Bean
    @ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
    public BackplaneStompRelay backplaneStompRelay(
            SimpMessagingTemplate messagingTemplate,
            ObjectMapper objectMapper) {
        return new BackplaneStompRelay(messagingTemplate, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "backplane", name = "enabled", havingValue = "true")
    public RedisMessageListenerContainer backplaneListenerContainer(
            @Qualifier("backplaneConnectionFactory") RedisConnectionFactory factory,
            BackplaneStompRelay relay,
            BackplaneProperties props) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);
        container.addMessageListener(relay, new ChannelTopic(props.getChannel()));
        return container;
    }

    // ── Helper ──────────────────────────────────────────────────────────────

    private LettuceConnectionFactory connectionFactory(String host, int port, String password) {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        if (password != null && !password.isBlank()) {
            config.setPassword(password);
        }
        // Ambos clusters ElastiCache (cache y backplane) tienen transit_encryption
        // habilitado en Ulink_Infra (elasticache-cache hardcoded true; backplane
        // via var.backplane_tls_enabled con default true). Sin useSsl() aqui, el
        // handshake TLS falla y el pod queda unready. disablePeerVerification()
        // porque los certificados AWS-managed no matchean el hostname master.*
        // sin configurar el truststore del CA de ElastiCache -- el transporte
        // sigue cifrado, solo no se autentica el servidor.
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .useSsl()
                .disablePeerVerification()
                .build();
        return new LettuceConnectionFactory(config, clientConfig);
    }
}
