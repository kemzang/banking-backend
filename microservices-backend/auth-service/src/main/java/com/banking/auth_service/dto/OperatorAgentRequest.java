package com.banking.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record OperatorAgentRequest(
        @JsonAlias("firstName") String prenom,
        @JsonAlias("lastName") String nom,
        String email,
        @JsonAlias("password") String motDePasse
) {
}
