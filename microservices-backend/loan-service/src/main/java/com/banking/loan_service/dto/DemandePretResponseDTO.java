package com.banking.loan_service.dto;

import com.banking.loan_service.entity.StatutDemande;
import java.math.BigDecimal;

public record DemandePretResponseDTO(
    Long id,
    Long clientId,
    BigDecimal montantDemande,
    int dureeMois,
    BigDecimal scoreRisque,
    StatutDemande statut
) {}