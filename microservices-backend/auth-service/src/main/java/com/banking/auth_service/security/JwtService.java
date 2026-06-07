package com.banking.auth_service.security;

import com.banking.auth_service.entity.Utilisateur;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

// Service responsable de la CREATION et de la VERIFICATION des jetons JWT.
@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    // La cle de signature, derivee du secret. Sert a signer ET a verifier.
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Genere un jeton signe pour un utilisateur.
    public String generateToken(Utilisateur u) {
        Date now = new Date();
        return Jwts.builder()
                .subject(u.getEmail())                                    // "qui" : l'email
                .claim("userId", u.getId().toString())                    // infos additionnelles
                .claim("roles", u.getRoles().stream().map(Enum::name).toList())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))       // date d'expiration
                .signWith(key())                                          // signature (anti-falsification)
                .compact();
    }

    // Extrait l'email (le "subject") contenu dans le jeton.
    public String extractEmail(String token) {
        return parse(token).getSubject();
    }

    // true si le jeton est bien signe et non expire.
    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Verifie la signature + decode le contenu. Leve une exception si invalide.
    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }
}
