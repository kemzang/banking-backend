package com.banking.loan_service.dto;

import java.math.BigDecimal;

public record ClientLoanRequestDTO(
        Long accountId,
        BigDecimal montantDemande,
        int dureeMois,
        String motif
) {
    public ClientLoanRequestDTO(BigDecimal montantDemande, int dureeMois, String motif) {
        this(null, montantDemande, dureeMois, motif);
    }
}
