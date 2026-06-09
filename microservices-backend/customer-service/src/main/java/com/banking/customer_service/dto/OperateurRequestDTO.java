package com.banking.customer_service.dto;

import com.banking.customer_service.entity.TypeOperateur;

public record OperateurRequestDTO(
        String nom,
        TypeOperateur type,
        String code
) {
}
