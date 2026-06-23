package com.banking.auth_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;

import com.banking.auth_service.entity.Role;

// Reponse renvoyee apres une connexion reussie : le jeton JWT.
public record AuthResponse(
        String token,
        String type,        // "Bearer"
        long expiresIn,     // duree de validite en secondes
        UserResponse user
) {
    @JsonProperty("accessToken")
    public String accessToken() {
        return token;
    }

    @JsonProperty("tokenType")
    public String tokenType() {
        return type;
    }

    @JsonProperty("userId")
    public UUID userId() {
        return user.id();
    }

    @JsonProperty("email")
    public String email() {
        return user.email();
    }

    @JsonProperty("roles")
    public Set<Role> roles() {
        return user.roles();
    }

    @JsonProperty("operatorId")
    public Long operatorId() {
        return user.operatorId();
    }
}
