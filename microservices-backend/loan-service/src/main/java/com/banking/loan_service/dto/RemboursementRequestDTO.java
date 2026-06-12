package com.banking.loan_service.dto;

import com.banking.loan_service.entity.MoyenPaiement;
import java.math.BigDecimal;

public record RemboursementRequestDTO(
    BigDecimal montant,
    MoyenPaiement moyenPaiement
) {}