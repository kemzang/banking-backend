package com.banking.auth_service.dto;

// Ce que l'utilisateur envoie pour s'inscrire.
public record RegisterRequest(
        String email,
        String motDePasse,
        String telephone
) {
}
