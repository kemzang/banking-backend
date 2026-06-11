package com.banking.account_service.dto;

import java.math.BigDecimal;

public record SoldeResponseDTO(
        Long compteId,
        String numeroCompte,
        BigDecimal solde,
        String devise
) {}
