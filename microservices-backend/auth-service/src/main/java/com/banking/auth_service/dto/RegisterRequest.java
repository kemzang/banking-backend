package com.banking.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

// Ce que l'utilisateur envoie pour s'inscrire.
public record RegisterRequest(
        String email,
        @JsonAlias("password")
        String motDePasse,
        String telephone,
        @JsonAlias("lastName")
        String nom,
        @JsonAlias("firstName")
        String prenom,
        Long operatorId
) {
}
