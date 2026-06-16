package com.banking.transaction_service.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declare l'exchange RabbitMQ utilise pour publier les evenements metier.
 * Active uniquement quand transaction.events.broker = rabbitmq.
 * L'exchange "banking.events" est celui ecoute par le notification-service (Node).
 */
@Configuration
@ConditionalOnProperty(name = "transaction.events.broker", havingValue = "rabbitmq")
public class RabbitEventConfig {

    public static final String EXCHANGE = "banking.events";

    @Bean
    public TopicExchange bankingEventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }
}
