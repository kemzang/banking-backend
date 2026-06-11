package com.banking.account_service.dto;

import com.banking.account_service.entity.TypeCompte;
import java.math.BigDecimal;

public record CompteRequestDTO(
        Long clientId,
        Long operateurId,
        TypeCompte type,
        String devise,
        BigDecimal plafondJournalier,
        BigDecimal decouvertAutorise
) {}
