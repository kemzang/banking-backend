package com.banking.auth_service.entity;

public enum Role {
    CLIENT,
    ADMIN_PLATFORM,
    OPERATOR_ADMIN,
    OPERATOR_AGENT,

    // Conserves temporairement pour permettre la migration des lignes existantes.
    @Deprecated
    ADMIN,
    @Deprecated
    OPERATEUR;

    public boolean isOperatorRole() {
        return this == OPERATOR_ADMIN || this == OPERATOR_AGENT;
    }
}
