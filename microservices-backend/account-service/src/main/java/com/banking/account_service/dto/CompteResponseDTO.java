package com.banking.account_service.dto;

import com.banking.account_service.entity.StatutCompte;
import com.banking.account_service.entity.TypeCompte;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompteResponseDTO(
        Long id,
        String numeroCompte,
        Long clientId,
        Long operateurId,
        TypeCompte type,
        BigDecimal solde,
        String devise,
        BigDecimal plafondJournalier,
        BigDecimal decouvertAutorise,
        StatutCompte statut,
        LocalDateTime dateOuverture
) {}
