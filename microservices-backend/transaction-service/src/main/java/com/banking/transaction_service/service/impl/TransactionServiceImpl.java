package com.banking.transaction_service.service.impl;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.StatutTransaction;
import com.banking.transaction_service.enums.TypeTransaction;
import com.banking.transaction_service.event.TransactionEventPublisher;
import com.banking.transaction_service.exception.DeviseIncompatibleException;
import com.banking.transaction_service.exception.InvalidTransactionException;
import com.banking.transaction_service.exception.TransactionLimitExceededException;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.service.TransactionService;
import com.banking.transaction_service.strategy.CommissionStrategy;
import com.banking.transaction_service.util.TransactionReferenceGenerator;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransactionServiceImpl implements TransactionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final TransactionReferenceGenerator referenceGenerator;
    private final AccountClient accountClient;
    private final CommissionStrategy commissionStrategy;
    private final TransactionEventPublisher eventPublisher;
    private final BigDecimal maxAmount;

    public TransactionServiceImpl(
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            TransactionReferenceGenerator referenceGenerator,
            AccountClient accountClient,
            CommissionStrategy commissionStrategy,
            TransactionEventPublisher eventPublisher,
            @Value("${transaction.limits.max-amount:1000000}") BigDecimal maxAmount
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.referenceGenerator = referenceGenerator;
        this.accountClient = accountClient;
        this.commissionStrategy = commissionStrategy;
        this.eventPublisher = eventPublisher;
        this.maxAmount = maxAmount;
    }

    @Override
    public TransactionResponseDTO depot(DepotRequestDTO request) {
        Transaction transaction = createTransaction(
                TypeTransaction.DEPOT,
                request.montant(),
                request.devise(),
                null,
                request.compteId(),
                null,
                BigDecimal.ZERO
        );
        try {
            accountClient.credit(request.compteId(), request.montant());
            return validate(transaction);
        } catch (RuntimeException exception) {
            reject(transaction, rejectionReason(exception));
            throw exception;
        }
    }

    @Override
    public TransactionResponseDTO retrait(RetraitRequestDTO request) {
        Transaction transaction = createTransaction(
                TypeTransaction.RETRAIT,
                request.montant(),
                request.devise(),
                request.compteId(),
                null,
                null,
                BigDecimal.ZERO
        );
        try {
            enforceLimit(request.montant());
            AccountResponseDTO account = accountClient.getById(request.compteId());
            ensureCurrency(request.devise(), account);
            transaction.setOperateurSourceId(account.operateurId());
            transactionRepository.save(transaction);

            accountClient.debit(request.compteId(), request.montant());
            return validate(transaction);
        } catch (RuntimeException exception) {
            reject(transaction, rejectionReason(exception));
            throw exception;
        }
    }

    @Override
    public TransactionResponseDTO transfert(TransfertRequestDTO request) {
        Transaction transaction = createTransaction(
                TypeTransaction.TRANSFERT,
                request.montant(),
                request.devise(),
                request.compteSourceId(),
                request.compteDestId(),
                request.motif(),
                BigDecimal.ZERO
        );

        boolean sourceDebited = false;
        try {
            if (request.compteSourceId().equals(request.compteDestId())) {
                throw new InvalidTransactionException(
                        "Les comptes source et destination doivent etre differents"
                );
            }
            enforceLimit(request.montant());

            AccountResponseDTO source = accountClient.getById(request.compteSourceId());
            AccountResponseDTO destination = accountClient.getById(request.compteDestId());
            ensureCurrency(request.devise(), source);
            ensureCurrency(request.devise(), destination);

            BigDecimal commission = commissionStrategy.calculate(
                    request.montant(),
                    source.operateurId(),
                    destination.operateurId()
            );
            transaction.setOperateurSourceId(source.operateurId());
            transaction.setOperateurDestId(destination.operateurId());
            transaction.setCommission(commission);
            transactionRepository.save(transaction);

            accountClient.debit(
                    request.compteSourceId(),
                    request.montant().add(commission)
            );
            sourceDebited = true;
            accountClient.credit(request.compteDestId(), request.montant());
            return validate(transaction);
        } catch (RuntimeException exception) {
            if (sourceDebited) {
                BigDecimal totalDebited =
                        transaction.getMontant().add(transaction.getCommission());
                reject(
                        transaction,
                        "Crédit destination échoué après débit source, compensation demandée"
                );
                publishCompensation(transaction, totalDebited);
            } else {
                reject(transaction, rejectionReason(exception));
            }
            throw exception;
        }
    }

    @Override
    public TransactionResponseDTO getById(Long id) {
        return transactionRepository.findById(id)
                .map(transactionMapper::toResponse)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Override
    public List<TransactionResponseDTO> getByCompte(Long accountId) {
        return transactionRepository
                .findByCompteSourceIdOrCompteDestIdOrderByDateOperationDesc(
                        accountId,
                        accountId
                )
                .stream()
                .map(transactionMapper::toResponse)
                .toList();
    }

    private Transaction createTransaction(
            TypeTransaction type,
            BigDecimal montant,
            String devise,
            Long compteSourceId,
            Long compteDestId,
            String motif,
            BigDecimal commission
    ) {
        validateRequest(montant, devise);
        Transaction transaction = new Transaction();
        transaction.setType(type);
        transaction.setMontant(montant);
        transaction.setDevise(devise.trim().toUpperCase());
        transaction.setCompteSourceId(compteSourceId);
        transaction.setCompteDestId(compteDestId);
        transaction.setMotif(motif);
        transaction.setCommission(commission);
        transaction.setStatut(StatutTransaction.INITIEE);

        Transaction saved = transactionRepository.saveAndFlush(transaction);
        saved.setReference(referenceGenerator.generate(saved.getId()));
        return transactionRepository.save(saved);
    }

    private void validateRequest(BigDecimal montant, String devise) {
        if (montant == null || montant.signum() <= 0) {
            throw new InvalidTransactionException(
                    "Le montant doit etre strictement positif"
            );
        }
        if (devise == null || devise.isBlank()) {
            throw new InvalidTransactionException("La devise est obligatoire");
        }
    }

    private void enforceLimit(BigDecimal montant) {
        if (montant.compareTo(maxAmount) > 0) {
            throw new TransactionLimitExceededException(maxAmount);
        }
    }

    private void ensureCurrency(String requestedCurrency, AccountResponseDTO account) {
        if (account == null || account.devise() == null
                || !account.devise().equalsIgnoreCase(requestedCurrency)) {
            throw new DeviseIncompatibleException("Devise incompatible");
        }
    }

    private TransactionResponseDTO validate(Transaction transaction) {
        transaction.setStatut(StatutTransaction.VALIDEE);
        Transaction saved = transactionRepository.save(transaction);
        publishSafely(
                () -> eventPublisher.publishTransactionCompleted(saved),
                saved,
                "transaction.completed"
        );
        return transactionMapper.toResponse(saved);
    }

    private void reject(Transaction transaction, String reason) {
        transaction.setStatut(StatutTransaction.REJETEE);
        transaction.setMotif(reason);
        Transaction saved = transactionRepository.save(transaction);
        publishSafely(
                () -> eventPublisher.publishTransactionRejected(saved),
                saved,
                "transaction.rejected"
        );
    }

    private String rejectionReason(RuntimeException exception) {
        if (exception instanceof com.banking.transaction_service.exception.SoldeInsuffisantException) {
            return "Solde insuffisant";
        }
        if (exception instanceof DeviseIncompatibleException) {
            return "Devise incompatible";
        }
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? "Echec de l'operation"
                : message;
    }

    private void publishCompensation(
            Transaction transaction,
            BigDecimal totalDebited
    ) {
        publishSafely(
                () -> eventPublisher.publishCompensationRequested(
                        transaction,
                        transaction.getCompteSourceId(),
                        totalDebited
                ),
                transaction,
                "transaction.compensation.requested"
        );
    }

    private void publishSafely(
            Runnable publication,
            Transaction transaction,
            String eventType
    ) {
        try {
            publication.run();
        } catch (RuntimeException publicationException) {
            LOGGER.error(
                    "Unable to publish {} for transaction {}",
                    eventType,
                    transaction.getReference(),
                    publicationException
            );
        }
    }
}
