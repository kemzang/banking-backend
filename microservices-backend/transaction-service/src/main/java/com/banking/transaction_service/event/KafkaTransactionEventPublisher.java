package com.banking.transaction_service.event;

import com.banking.transaction_service.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@ConditionalOnProperty(name = "transaction.events.broker", havingValue = "kafka")
public class KafkaTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(KafkaTransactionEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String completedTopic;
    private final String rejectedTopic;
    private final String compensationTopic;

    public KafkaTransactionEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${transaction.events.topic-completed}") String completedTopic,
            @Value("${transaction.events.topic-rejected}") String rejectedTopic,
            @Value("${transaction.events.topic-compensation}") String compensationTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.completedTopic = completedTopic;
        this.rejectedTopic = rejectedTopic;
        this.compensationTopic = compensationTopic;
    }

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        send(completedTopic, transaction, TransactionEventFactory.completed(transaction));
    }

    @Override
    public void publishTransactionRejected(Transaction transaction) {
        send(rejectedTopic, transaction, TransactionEventFactory.rejected(transaction));
    }

    @Override
    public void publishCompensationRequested(
            Transaction transaction,
            Long compteId,
            BigDecimal montant
    ) {
        send(
                compensationTopic,
                transaction,
                TransactionEventFactory.compensation(transaction, compteId, montant)
        );
    }

    private void send(String topic, Transaction transaction, TransactionEvent event) {
        kafkaTemplate.send(topic, transaction.getReference(), event)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        LOGGER.error(
                                "Unable to send {} for transaction {} to topic {}",
                                event.eventType(),
                                transaction.getReference(),
                                topic,
                                exception
                        );
                    }
                });
    }
}
