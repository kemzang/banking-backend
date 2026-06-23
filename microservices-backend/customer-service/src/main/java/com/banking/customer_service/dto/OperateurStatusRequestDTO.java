package com.banking.customer_service.dto;

import com.banking.customer_service.entity.StatutOperateur;
import com.fasterxml.jackson.annotation.JsonAlias;

public record OperateurStatusRequestDTO(
        @JsonAlias("statut") StatutOperateur status
) {
}
