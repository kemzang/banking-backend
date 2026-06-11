package com.banking.loan_service.dto;

import java.math.BigDecimal;

public record DemandePretRequestDTO(
    Long clientId,
    BigDecimal montantDemande,
    int dureeMois,
    String motif
) {}