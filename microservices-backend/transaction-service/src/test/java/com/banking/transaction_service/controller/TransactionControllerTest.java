package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.TransactionResponseDTO;
import com.banking.transaction_service.enums.StatutTransaction;
import com.banking.transaction_service.enums.TypeTransaction;
import com.banking.transaction_service.exception.GlobalExceptionHandler;
import com.banking.transaction_service.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    @Mock
    private com.banking.transaction_service.client.AccountClient accountClient;

    @Mock
    private com.banking.transaction_service.client.CustomerClient customerClient;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TransactionController(transactionService, accountClient, customerClient))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void depositReturnsCreatedTransaction() throws Exception {
        when(transactionService.depot(any())).thenReturn(response(TypeTransaction.DEPOT));

        mockMvc.perform(post("/api/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteId":1,"montant":50000,"devise":"XAF"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("DEPOT"));
    }

    @Test
    void withdrawReturnsCreatedTransaction() throws Exception {
        when(transactionService.retrait(any())).thenReturn(response(TypeTransaction.RETRAIT));

        mockMvc.perform(post("/api/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteId":1,"montant":10000,"devise":"XAF"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("RETRAIT"));
    }

    @Test
    void transferReturnsCreatedTransaction() throws Exception {
        when(transactionService.transfert(any()))
                .thenReturn(response(TypeTransaction.TRANSFERT));

        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"compteSourceId":1,"compteDestId":2,
                                "montant":25000,"devise":"XAF",
                                "motif":"Paiement fournisseur"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("TRANSFERT"));
    }

    @Test
    void getByIdReturnsTransaction() throws Exception {
        when(transactionService.getById(1L))
                .thenReturn(response(TypeTransaction.DEPOT));

        mockMvc.perform(get("/api/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getByAccountReturnsHistory() throws Exception {
        when(transactionService.getByCompte(1L))
                .thenReturn(List.of(response(TypeTransaction.DEPOT)));

        mockMvc.perform(get("/api/transactions").param("accountId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reference").value("TX-2026-000001"));
    }

    private TransactionResponseDTO response(TypeTransaction type) {
        return new TransactionResponseDTO(
                1L,
                "TX-2026-000001",
                type,
                new BigDecimal("1000.00"),
                "XAF",
                BigDecimal.ZERO,
                StatutTransaction.VALIDEE,
                null,
                LocalDateTime.of(2026, 6, 11, 10, 0)
        );
    }
}
