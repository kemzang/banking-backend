package com.banking.loan_service.dto;

import com.banking.loan_service.entity.StatutPret;
import java.math.BigDecimal;

public record PretResponseDTO(
    Long id,
    Long clientId,
    BigDecimal montantAccorde,
    BigDecimal tauxInteret,
    int dureeMois,
    BigDecimal capitalRestant,
    StatutPret statut
) {}