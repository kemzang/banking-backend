package com.banking.account_service.dto;

import java.math.BigDecimal;

/**
 * DTO pour les opérations de crédit / débit internes (appelées par transaction-service).
 * Le champ 'reference' est la référence de la transaction (ex: TX-2026-000042).
 */
public record MouvementDTO(
        BigDecimal montant,
        String reference
) {}
