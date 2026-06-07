package com.banking.auth_service.dto;

// Ce que l'utilisateur envoie pour se connecter.
public record LoginRequest(
        String email,
        String motDePasse
) {
}
