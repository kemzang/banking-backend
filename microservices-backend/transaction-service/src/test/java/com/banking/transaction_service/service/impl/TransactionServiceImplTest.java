package com.banking.transaction_service.service.impl;

import com.banking.transaction_service.client.AccountClient;
import com.banking.transaction_service.dto.AccountResponseDTO;
import com.banking.transaction_service.dto.DepotRequestDTO;
import com.banking.transaction_service.dto.RetraitRequestDTO;
import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.dto.TransfertRequestDTO;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.enums.StatutTransaction;
import com.banking.transaction_service.event.TransactionEventPublisher;
import com.banking.transaction_service.exception.AccountServiceException;
import com.banking.transaction_service.exception.DeviseIncompatibleException;
import com.banking.transaction_service.exception.SoldeInsuffisantException;
import com.banking.transaction_service.exception.TransactionLimitExceededException;
import com.banking.transaction_service.exception.TransactionNotFoundException;
import com.banking.transaction_service.mapper.TransactionMapper;
import com.banking.transaction_service.repository.TransactionRepository;
import com.banking.transaction_service.strategy.PercentageCommissionStrategy;
import com.banking.transaction_service.util.TransactionReferenceGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private TransactionEventPublisher eventPublisher;

    private TransactionServiceImpl transactionService;
    private List<StatutTransaction> savedStatuses;

    @BeforeEach
    void setUp() {
        AtomicLong ids = new AtomicLong();
        savedStatuses = new ArrayList<>();
        lenient().when(transactionRepository.saveAndFlush(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    transaction.setId(ids.incrementAndGet());
                    return transaction;
                });
        lenient().when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction transaction = invocation.getArgument(0);
                    savedStatuses.add(transaction.getStatut());
                    return transaction;
                });

        transactionService = new TransactionServiceImpl(
                transactionRepository,
                new TransactionMapper(),
                new TransactionReferenceGenerator(),
                accountClient,
                new PercentageCommissionStrategy(
                        new BigDecimal("0.005"),
                        BigDecimal.ZERO
                ),
                eventPublisher,
                new BigDecimal("1000000")
        );
    }

    @Test
    void depotCreditsAccountAndValidatesTransaction() {
        TransactionResponseDTO response = transactionService.depot(
                new DepotRequestDTO(10L, new BigDecimal("25000.00"), "xaf")
        );

        verify(accountClient).credit(10L, new BigDecimal("25000.00"));
        assertEquals(StatutTransaction.VALIDEE, response.statut());
        assertEquals("TX-" + java.time.Year.now().getValue() + "-000001", response.reference());
        assertEquals("XAF", response.devise());
        verify(eventPublisher).publishTransactionCompleted(any(Transaction.class));
    }

    @Test
    void remoteFailureRejectsTransactionAndRethrowsError() {
        AccountServiceException failure = new AccountServiceException(
                HttpStatus.CONFLICT,
                "Solde insuffisant",
                null
        );
        org.mockito.Mockito.doThrow(failure)
                .when(accountClient)
                .credit(10L, new BigDecimal("100.00"));

        AccountServiceException thrown = assertThrows(
                AccountServiceException.class,
                () -> transactionService.depot(
                        new DepotRequestDTO(10L, new BigDecimal("100.00"), "XAF")
                )
        );

        assertEquals(HttpStatus.CONFLICT, thrown.getStatusCode());
        assertEquals(
                List.of(StatutTransaction.INITIEE, StatutTransaction.REJETEE),
                savedStatuses
        );
        verify(eventPublisher).publishTransactionRejected(any(Transaction.class));
    }

    @Test
    void transfertDebitsAmountWithCommissionBeforeCredit() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));
        when(accountClient.getById(2L))
                .thenReturn(new AccountResponseDTO(2L, 20L, "XAF"));

        TransactionResponseDTO response = transactionService.transfert(
                new TransfertRequestDTO(
                        1L,
                        2L,
                        new BigDecimal("1000.00"),
                        "XAF",
                        "Paiement"
                )
        );

        InOrder operations = inOrder(accountClient);
        operations.verify(accountClient).debit(1L, new BigDecimal("1005.00"));
        operations.verify(accountClient).credit(2L, new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("5.00"), response.commission());
        assertEquals(StatutTransaction.VALIDEE, response.statut());
        verify(eventPublisher).publishTransactionCompleted(any(Transaction.class));
    }

    @Test
    void retraitAboveLimitIsRejectedWithoutDebit() {
        assertThrows(
                TransactionLimitExceededException.class,
                () -> transactionService.retrait(
                        new RetraitRequestDTO(
                                1L,
                                new BigDecimal("1000000.01"),
                                "XAF"
                        )
                )
        );

        verify(accountClient, never()).getById(1L);
        verify(accountClient, never()).debit(
                org.mockito.ArgumentMatchers.anyLong(),
                any(BigDecimal.class)
        );
        assertEquals(
                StatutTransaction.REJETEE,
                savedStatuses.get(savedStatuses.size() - 1)
        );
    }

    @Test
    void retraitWithDifferentCurrencyIsRejected() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "EUR"));

        assertThrows(
                DeviseIncompatibleException.class,
                () -> transactionService.retrait(
                        new RetraitRequestDTO(1L, new BigDecimal("500.00"), "XAF")
                )
        );

        verify(accountClient, never()).debit(
                org.mockito.ArgumentMatchers.anyLong(),
                any(BigDecimal.class)
        );
        assertEquals(
                StatutTransaction.REJETEE,
                savedStatuses.get(savedStatuses.size() - 1)
        );
    }

    @Test
    void sameOperatorTransferHasNoCommission() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));
        when(accountClient.getById(2L))
                .thenReturn(new AccountResponseDTO(2L, 10L, "XAF"));

        TransactionResponseDTO response = transactionService.transfert(
                new TransfertRequestDTO(
                        1L,
                        2L,
                        new BigDecimal("1000.00"),
                        "XAF",
                        null
                )
        );

        verify(accountClient).debit(1L, new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("0.00"), response.commission());
    }

    @Test
    void creditFailureAfterDebitRequestsSagaCompensation() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));
        when(accountClient.getById(2L))
                .thenReturn(new AccountResponseDTO(2L, 20L, "XAF"));
        AccountServiceException failure = new AccountServiceException(
                HttpStatus.BAD_GATEWAY,
                "Echec du credit",
                null
        );
        doThrow(failure).when(accountClient)
                .credit(2L, new BigDecimal("1000.00"));

        assertThrows(
                AccountServiceException.class,
                () -> transactionService.transfert(
                        new TransfertRequestDTO(
                                1L,
                                2L,
                                new BigDecimal("1000.00"),
                                "XAF",
                                "Paiement"
                        )
                )
        );

        verify(accountClient).debit(1L, new BigDecimal("1005.00"));
        verify(eventPublisher).publishTransactionRejected(
                org.mockito.ArgumentMatchers.argThat(
                        transaction -> "Crédit destination échoué après débit source, compensation demandée"
                                .equals(transaction.getMotif())
                )
        );
        verify(eventPublisher).publishCompensationRequested(
                any(Transaction.class),
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(new BigDecimal("1005.00"))
        );
        assertEquals(
                StatutTransaction.REJETEE,
                savedStatuses.get(savedStatuses.size() - 1)
        );
    }

    @Test
    void retraitDebitsAccountAndValidatesTransaction() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));

        TransactionResponseDTO response = transactionService.retrait(
                new RetraitRequestDTO(1L, new BigDecimal("10000.00"), "XAF")
        );

        verify(accountClient).debit(1L, new BigDecimal("10000.00"));
        assertEquals(StatutTransaction.VALIDEE, response.statut());
    }

    @Test
    void retraitWithInsufficientBalanceIsRejected() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));
        doThrow(new SoldeInsuffisantException("Solde insuffisant"))
                .when(accountClient)
                .debit(1L, new BigDecimal("10000.00"));

        assertThrows(
                SoldeInsuffisantException.class,
                () -> transactionService.retrait(
                        new RetraitRequestDTO(
                                1L,
                                new BigDecimal("10000.00"),
                                "XAF"
                        )
                )
        );

        verify(eventPublisher).publishTransactionRejected(
                org.mockito.ArgumentMatchers.argThat(
                        transaction -> "Solde insuffisant".equals(transaction.getMotif())
                )
        );
    }

    @Test
    void transfertDoesNotCreditWhenDebitFails() {
        when(accountClient.getById(1L))
                .thenReturn(new AccountResponseDTO(1L, 10L, "XAF"));
        when(accountClient.getById(2L))
                .thenReturn(new AccountResponseDTO(2L, 20L, "XAF"));
        doThrow(new SoldeInsuffisantException("Solde insuffisant"))
                .when(accountClient)
                .debit(1L, new BigDecimal("1005.00"));

        assertThrows(
                SoldeInsuffisantException.class,
                () -> transactionService.transfert(
                        new TransfertRequestDTO(
                                1L,
                                2L,
                                new BigDecimal("1000.00"),
                                "XAF",
                                null
                        )
                )
        );

        verify(accountClient, never()).credit(
                org.mockito.ArgumentMatchers.anyLong(),
                any(BigDecimal.class)
        );
        verify(eventPublisher, never()).publishCompensationRequested(
                any(Transaction.class),
                org.mockito.ArgumentMatchers.anyLong(),
                any(BigDecimal.class)
        );
    }

    @Test
    void getByIdReturnsMappedTransaction() {
        Transaction transaction = transaction(7L, 1L, 2L);
        when(transactionRepository.findById(7L)).thenReturn(Optional.of(transaction));

        TransactionResponseDTO response = transactionService.getById(7L);

        assertEquals(7L, response.id());
        assertEquals("TX-2026-000007", response.reference());
    }

    @Test
    void getByIdThrowsWhenTransactionDoesNotExist() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(
                TransactionNotFoundException.class,
                () -> transactionService.getById(99L)
        );
    }

    @Test
    void getByCompteReturnsSourceAndDestinationTransactions() {
        when(transactionRepository
                .findByCompteSourceIdOrCompteDestIdOrderByDateOperationDesc(1L, 1L))
                .thenReturn(List.of(
                        transaction(1L, 1L, 2L),
                        transaction(2L, 3L, 1L)
                ));

        List<TransactionResponseDTO> responses = transactionService.getByCompte(1L);

        assertEquals(2, responses.size());
        assertEquals(List.of(1L, 2L), responses.stream()
                .map(TransactionResponseDTO::id)
                .toList());
    }

    private Transaction transaction(Long id, Long sourceId, Long destinationId) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setReference("TX-2026-%06d".formatted(id));
        transaction.setType(com.banking.transaction_service.enums.TypeTransaction.TRANSFERT);
        transaction.setMontant(new BigDecimal("1000.00"));
        transaction.setDevise("XAF");
        transaction.setCompteSourceId(sourceId);
        transaction.setCompteDestId(destinationId);
        transaction.setCommission(new BigDecimal("5.00"));
        transaction.setStatut(StatutTransaction.VALIDEE);
        transaction.setDateOperation(LocalDateTime.of(2026, 6, 11, 10, 0));
        return transaction;
    }
}
