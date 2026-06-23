package com.banking.loan_service.dto;

import java.math.BigDecimal;

public record DemandePretRequestDTO(
    Long clientId,
    Long accountId,
    Long operatorId,
    BigDecimal montantDemande,
    int dureeMois,
    String motif
) {
    /** Compatibilite avec les appels internes historiques; les endpoints publics
     * construisent toujours la forme securisee avec compte et operateur. */
    public DemandePretRequestDTO(Long clientId, BigDecimal montantDemande, int dureeMois, String motif) {
        this(clientId, null, null, montantDemande, dureeMois, motif);
    }
}
