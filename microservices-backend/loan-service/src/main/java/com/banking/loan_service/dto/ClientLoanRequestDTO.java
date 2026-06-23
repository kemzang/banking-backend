package com.banking.loan_service.dto;

import java.math.BigDecimal;

public record ClientLoanRequestDTO(
        BigDecimal montantDemande,
        int dureeMois,
        String motif
) {
}
