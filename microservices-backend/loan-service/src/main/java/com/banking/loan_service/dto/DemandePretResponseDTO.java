package com.banking.loan_service.dto;

import com.banking.loan_service.entity.StatutDemande;
import java.math.BigDecimal;

public record DemandePretResponseDTO(
    Long id,
    Long clientId,
    Long accountId,
    Long operatorId,
    BigDecimal montantDemande,
    int dureeMois,
    BigDecimal scoreRisque,
    StatutDemande statut
) {
    public DemandePretResponseDTO(Long id, Long clientId, BigDecimal montantDemande,
            int dureeMois, BigDecimal scoreRisque, StatutDemande statut) {
        this(id, clientId, null, null, montantDemande, dureeMois, scoreRisque, statut);
    }
}
