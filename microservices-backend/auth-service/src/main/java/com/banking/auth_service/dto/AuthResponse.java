package com.banking.auth_service.dto;

// Reponse renvoyee apres une connexion reussie : le jeton JWT.
public record AuthResponse(
        String token,
        String type,        // "Bearer"
        long expiresIn      // duree de validite en secondes
) {
}
