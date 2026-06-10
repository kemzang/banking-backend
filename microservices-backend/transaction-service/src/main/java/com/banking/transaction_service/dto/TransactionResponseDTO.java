package com.banking.transaction_service.dto;

import com.banking.transaction_service.enums.StatutTransaction;
import com.banking.transaction_service.enums.TypeTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDTO(
        Long id,
        String reference,
        TypeTransaction type,
        BigDecimal montant,
        String devise,
        BigDecimal commission,
        StatutTransaction statut,
        String motif,
        LocalDateTime dateOperation
) {
}
