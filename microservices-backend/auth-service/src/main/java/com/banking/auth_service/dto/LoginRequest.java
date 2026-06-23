package com.banking.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

// Ce que l'utilisateur envoie pour se connecter.
public record LoginRequest(
        String email,
        @JsonAlias("password")
        String motDePasse,
        LoginType loginType
) {
}
