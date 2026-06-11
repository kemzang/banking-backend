package com.banking.loan_service.dto;

import java.math.BigDecimal;

public record DecisionRequestDTO(
    boolean approuver,
    BigDecimal tauxInteret,
    Long compteId,
    String motifRejet
) {}