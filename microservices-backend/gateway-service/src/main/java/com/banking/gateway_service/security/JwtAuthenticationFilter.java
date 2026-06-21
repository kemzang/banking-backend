package com.banking.gateway_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Filtre GLOBAL : s'execute pour CHAQUE requete qui traverse la gateway.
// Il joue le role de "videur" : verifie le JWT a l'entree, bloque sinon.
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // Routes accessibles SANS jeton (inscription / connexion).
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/google"
    );

    private final SecretKey key;

    // La gateway doit utiliser EXACTEMENT le meme secret que auth-service
    // pour pouvoir verifier la signature des jetons.
    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Route publique -> on laisse passer sans verification
        if (isPublic(path)) {
            return chain.filter(exchange);
        }

        // 2. Pas de header Bearer -> 401
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange);
        }

        // 3. Verification de la signature + extraction des infos
        String token = authHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 4. On propage l'identite aux services en aval via des en-tetes
            //    (ils n'ont plus a re-verifier le jeton : ils font confiance a la gateway)
            String userId = String.valueOf(claims.get("userId"));
            String userEmail = claims.getSubject();
            String userRoles = String.valueOf(claims.get("roles"));
            LOG.info("JWT validated - userId: {}, email: {}, roles: {}", userId, userEmail, userRoles);

            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Email", userEmail)
                    .header("X-User-Id", userId)
                    .header("X-User-Roles", userRoles)
                    .build();

            LOG.info("Forwarding headers X-User-Email={}, X-User-Id={}, X-User-Roles={} for path {}",
                    userEmail, userId, userRoles, path);
            return chain.filter(exchange.mutate().request(mutated).build());

        } catch (Exception e) {
            // jeton expire, mal signe, falsifie... -> 401
            return unauthorized(exchange);
        }
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    // Ordre negatif = ce filtre s'execute TOT (avant le routage vers le service).
    @Override
    public int getOrder() {
        return -1;
    }
}
