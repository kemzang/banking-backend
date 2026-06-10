package com.banking.transaction_service.event;

import com.banking.transaction_service.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@ConditionalOnProperty(
        name = "transaction.events.broker",
        havingValue = "log",
        matchIfMissing = true
)
public class LogTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LogTransactionEventPublisher.class);

    @Override
    public void publishTransactionCompleted(Transaction transaction) {
        LOGGER.info("Business event: {}", TransactionEventFactory.completed(transaction));
    }

    @Override
    public void publishTransactionRejected(Transaction transaction) {
        LOGGER.info("Business event: {}", TransactionEventFactory.rejected(transaction));
    }

    @Override
    public void publishCompensationRequested(
            Transaction transaction,
            Long compteId,
            BigDecimal montant
    ) {
        LOGGER.warn(
                "Business event: {}",
                TransactionEventFactory.compensation(transaction, compteId, montant)
        );
    }
}
