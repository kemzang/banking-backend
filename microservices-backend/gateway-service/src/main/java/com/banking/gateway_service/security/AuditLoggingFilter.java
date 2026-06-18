package com.banking.gateway_service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Traçabilité / audit : journalise CHAQUE requête traversant la gateway
 * (méthode, chemin, utilisateur, statut, durée). Centralise l'audit des
 * accès a l'API au point d'entrée unique de la plateforme.
 */
@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String user = request.getHeaders().getFirst("X-User-Email");
        long start = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            var status = exchange.getResponse().getStatusCode();
            long duree = System.currentTimeMillis() - start;
            AUDIT.info("method={} path={} user={} status={} dureeMs={}",
                    request.getMethod(),
                    request.getURI().getPath(),
                    user != null ? user : "anonymous",
                    status != null ? status.value() : 0,
                    duree);
        }));
    }

    // Apres le filtre JWT (-1) pour disposer de l'en-tete X-User-Email.
    @Override
    public int getOrder() {
        return 0;
    }
}
