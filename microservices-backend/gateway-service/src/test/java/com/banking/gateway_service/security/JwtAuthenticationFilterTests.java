package com.banking.gateway_service.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTests {

    private static final String SECRET =
            "test-secret-key-with-at-least-256-bits-for-jwt-signature";

    @Test
    void replacesSpoofedIdentityHeadersAndPropagatesOperatorId() {
        String token = Jwts.builder()
                .subject("agent@expressunion.cm")
                .claim("userId", "user-42")
                .claim("roles", List.of("OPERATOR_AGENT"))
                .claim("operatorId", 42L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "spoofed")
                        .header("X-Operator-Id", "999")
                        .build()
        );
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        new JwtAuthenticationFilter(SECRET)
                .filter(exchange, forwardedExchange -> {
                    forwardedRequest.set(forwardedExchange.getRequest());
                    return Mono.empty();
                })
                .block();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-42");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("agent@expressunion.cm");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("OPERATOR_AGENT");
        assertThat(headers.getFirst("X-Operator-Id")).isEqualTo("42");
    }

    @Test
    void rejectsSignedTokenWithoutRequiredIdentityClaims() {
        String token = Jwts.builder()
                .subject("incomplete@example.cm")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build()
        );

        new JwtAuthenticationFilter(SECRET)
                .filter(exchange, forwardedExchange -> Mono.error(new AssertionError("La requete ne doit pas etre transmise")))
                .block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
