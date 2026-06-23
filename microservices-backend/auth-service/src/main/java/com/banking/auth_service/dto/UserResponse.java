package com.banking.auth_service.dto;

import com.banking.auth_service.entity.Role;
import java.util.Set;
import java.util.UUID;

// Infos d'un utilisateur exposees vers l'exterieur (jamais le mot de passe !).
public record UserResponse(
        UUID id,
        String email,
        Set<Role> roles,
        Long operatorId,
        String firstName,
        String lastName
) {
}
