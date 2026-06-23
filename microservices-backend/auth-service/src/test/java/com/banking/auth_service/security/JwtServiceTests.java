package com.banking.auth_service.security;

import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTests {

    private static final String SECRET =
            "test-secret-key-with-at-least-256-bits-for-jwt-signature";

    @Test
    void includesOperatorIdentityInToken() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3_600_000L);

        UUID userId = UUID.randomUUID();
        Utilisateur user = Utilisateur.builder()
                .id(userId)
                .email("agent@expressunion.cm")
                .motDePasse("hash")
                .operatorId(42L)
                .roles(new HashSet<>(Set.of(Role.OPERATOR_AGENT)))
                .build();

        String token = jwtService.generateToken(user);
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("agent@expressunion.cm");
        assertThat(claims.get("userId", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("operatorId", Integer.class)).isEqualTo(42);
        assertThat(claims.get("roles", java.util.List.class)).containsExactly("OPERATOR_AGENT");
    }
}
