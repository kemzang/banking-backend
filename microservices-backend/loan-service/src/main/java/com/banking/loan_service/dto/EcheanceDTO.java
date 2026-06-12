package com.banking.loan_service.dto;

import com.banking.loan_service.entity.StatutEcheance;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EcheanceDTO(
    int numero,
    LocalDate dateEcheance,
    BigDecimal montantCapital,
    BigDecimal montantInteret,
    BigDecimal montantTotal,
    StatutEcheance statut
) {}