package com.banking.transaction_service.event;

import com.banking.transaction_service.config.RabbitEventConfig;
import com.banking.transaction_service.entity.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Publie les evenements de transaction vers RabbitMQ (exchange "banking.events").
 * Active quand transaction.events.broker = rabbitmq.
 * Le payload JSON est aligne sur ce qu'attend le notification-service (Node) :
 * reference, type, montant, devise, statut, compteSourceId, compteDestId.
 */
@Service
@ConditionalOnProperty(name = "transaction.events.broker", havingValue = "rabbitmq")
public class RabbitTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(RabbitTransactionEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${transaction.events.topic-completed:transaction.completed}")
    private String topicCompleted;
    @Value("${transaction.events.topic-rejected:transaction.rejected}")
    private String topicRejected;
    @Value("${transaction.events.topic-compensation:transaction.compensation.requested}")
    private String topicCompensation;

    public RabbitTransactionEventPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishTransactionCompleted(Transaction tx) {
        envoyer(topicCompleted, payload(tx));
    }

    @Override
    public void publishTransactionRejected(Transaction tx) {
        envoyer(topicRejected, payload(tx));
    }

    @Override
    public void publishCompensationRequested(Transaction tx, Long compteId, BigDecimal montant) {
        Map<String, Object> p = payload(tx);
        p.put("compteCompensation", compteId);
        p.put("montantCompensation", montant);
        envoyer(topicCompensation, p);
    }

    private Map<String, Object> payload(Transaction tx) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("transactionId", tx.getId());
        p.put("reference", tx.getReference());
        p.put("type", tx.getType() != null ? tx.getType().name() : null);
        p.put("montant", tx.getMontant());
        p.put("devise", tx.getDevise());
        p.put("commission", tx.getCommission());
        p.put("statut", tx.getStatut() != null ? tx.getStatut().name() : null);
        p.put("compteSourceId", tx.getCompteSourceId());
        p.put("compteDestId", tx.getCompteDestId());
        p.put("dateOperation", String.valueOf(tx.getDateOperation()));
        return p;
    }

    private void envoyer(String routingKey, Map<String, Object> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            rabbitTemplate.convertAndSend(RabbitEventConfig.EXCHANGE, routingKey, json);
            LOGGER.info("Evenement publie -> {} : {}", routingKey, json);
        } catch (Exception e) {
            // On ne casse pas la transaction si la publication echoue
            LOGGER.error("Echec publication evenement {} : {}", routingKey, e.getMessage());
        }
    }
}
