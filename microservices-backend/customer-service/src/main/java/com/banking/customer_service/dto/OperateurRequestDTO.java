package com.banking.customer_service.dto;

import com.banking.customer_service.entity.TypeOperateur;
import com.banking.customer_service.entity.StatutOperateur;
import com.fasterxml.jackson.annotation.JsonAlias;

public record OperateurRequestDTO(
        @JsonAlias("name")
        String nom,
        TypeOperateur type,
        String code,
        @JsonAlias("status")
        StatutOperateur statut
) {
}
