package com.patricia.comunicacion.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.Jackson2JavaTypeMapper;

/**
 * Configuración de mensajería con Parches Core vía RabbitMQ.
 * <p>
 * Los nombres del exchange y de los routing keys deben coincidir EXACTAMENTE
 * con los definidos en el RabbitMQConfig de Parches — son el contrato entre
 * los dos microservicios. Comunicación re-declara el exchange (operación
 * idempotente en RabbitMQ siempre que la configuración coincida) para no
 * depender de que Parches haya arrancado primero, y declara sus propias
 * colas — cada consumidor es responsable de declarar y enlazar su propia
 * cola al exchange, ese es el patrón estándar en AMQP.
 */
@Configuration
public class RabbitMQConfig {

    // ---------- Exchange compartido con Parches ----------
    public static final String PARCHE_EXCHANGE = "parche.events";

    // ---------- Inbound: eventos que Parches publica y nosotros consumimos ----------
    public static final String PARCHE_CREATED_ROUTING_KEY          = "parche.created";
    public static final String PARCHE_MEMBER_JOINED_ROUTING_KEY    = "parche.member.joined";
    public static final String PARCHE_MEMBER_EXPELLED_ROUTING_KEY  = "parche.member.expelled";

    public static final String PARCHE_CREATED_QUEUE         = "comunicacion.parche.created.queue";
    public static final String PARCHE_MEMBER_JOINED_QUEUE   = "comunicacion.parche.member.joined.queue";
    public static final String PARCHE_MEMBER_EXPELLED_QUEUE = "comunicacion.parche.member.expelled.queue";

    // ---------- Outbound: respuesta que nosotros publicamos y Parches consume ----------
    public static final String COMMUNICATION_READY_ROUTING_KEY = "parche.communication.ready";

    // ---------- Exchange propio: eventos que Comunicación emite hacia Notificaciones ----------
    // Reemplaza el topic de Kafka chat.pendiente — Notificaciones (cuando exista)
    // declara su propia cola enlazada a este exchange, igual que nosotros
    // hacemos con el exchange de Parches.
    public static final String COMUNICACION_EXCHANGE = "comunicacion.events";
    public static final String CHAT_PENDIENTE_ROUTING_KEY = "chat.pendiente";

    @Bean
    public TopicExchange parcheExchange() {
        return new TopicExchange(PARCHE_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange comunicacionExchange() {
        return new TopicExchange(COMUNICACION_EXCHANGE, true, false);
    }

    @Bean
    public Queue parcheCreatedQueue() {
        return new Queue(PARCHE_CREATED_QUEUE, true);
    }

    @Bean
    public Queue parcheMemberJoinedQueue() {
        return new Queue(PARCHE_MEMBER_JOINED_QUEUE, true);
    }

    @Bean
    public Queue parcheMemberExpelledQueue() {
        return new Queue(PARCHE_MEMBER_EXPELLED_QUEUE, true);
    }

    @Bean
    public Binding parcheCreatedBinding() {
        return BindingBuilder.bind(parcheCreatedQueue())
                .to(parcheExchange())
                .with(PARCHE_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding parcheMemberJoinedBinding() {
        return BindingBuilder.bind(parcheMemberJoinedQueue())
                .to(parcheExchange())
                .with(PARCHE_MEMBER_JOINED_ROUTING_KEY);
    }

    @Bean
    public Binding parcheMemberExpelledBinding() {
        return BindingBuilder.bind(parcheMemberExpelledQueue())
                .to(parcheExchange())
                .with(PARCHE_MEMBER_EXPELLED_ROUTING_KEY);
    }

    /**
     * IMPORTANTE: TypePrecedence.INFERRED hace que el converter use el tipo
     * del parámetro del método @RabbitListener para deserializar, en vez de
     * confiar en el header __TypeId__ (que por defecto trae el nombre
     * completo de la clase Java del EMISOR). Sin esto, cuando publiquemos
     * CommunicationReadyEvent desde el paquete com.patricia.comunicacion...,
     * el listener de Parches (cuya clase vive en ingprompt.patricia.parches...)
     * fallaría al deserializar porque esa clase no existe en su classpath.
     * <p>
     * Esta misma línea debe agregarse también en el jsonMessageConverter()
     * de Parches para que la comunicación funcione en ambos sentidos.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypePrecedence(Jackson2JavaTypeMapper.TypePrecedence.INFERRED);
        return converter;
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
