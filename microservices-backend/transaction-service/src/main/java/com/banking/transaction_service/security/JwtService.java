package com.banking.transaction_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

// Verifie les jetons JWT emis par auth-service (cle secrete partagee).
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            Claims c = parse(token);
            return c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Object roles = parse(token).get("roles");
        if (roles instanceof List<?> l) {
            return l.stream().map(Object::toString).toList();
        }
        return List.of();
    }

    public Long extractOperatorId(String token) {
        Object op = parse(token).get("operatorId");
        if (op instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
