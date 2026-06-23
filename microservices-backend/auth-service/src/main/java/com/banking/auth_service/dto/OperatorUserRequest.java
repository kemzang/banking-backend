package com.banking.auth_service.dto;

import com.banking.auth_service.entity.Role;
import com.fasterxml.jackson.annotation.JsonAlias;

public record OperatorUserRequest(
        @JsonAlias("firstName") String prenom,
        @JsonAlias("lastName") String nom,
        String email,
        @JsonAlias("password") String motDePasse,
        Role role,
        Long operatorId
) {
}
