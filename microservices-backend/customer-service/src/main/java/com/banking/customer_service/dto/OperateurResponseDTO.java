package com.banking.customer_service.dto;

import com.banking.customer_service.entity.TypeOperateur;
import com.banking.customer_service.entity.StatutOperateur;

public record OperateurResponseDTO(
        Long id,
        String nom,
        TypeOperateur type,
        String code,
        StatutOperateur statut
) {
}
