package com.banking.auth_service.dto;

// Corps pour POST /api/auth/google : le jeton d'identite renvoye par Google cote front.
public record GoogleLoginRequest(String idToken) {
}
