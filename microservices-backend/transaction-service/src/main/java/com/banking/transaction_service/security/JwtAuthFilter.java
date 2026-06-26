package com.banking.transaction_service.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// Lit le JWT du header Authorization, extrait email + roles + operatorId,
// et peuple le SecurityContext Spring.
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                if (jwtService.isValid(token)) {
                    String email = jwtService.extractEmail(token);
                    List<String> roles = jwtService.extractRoles(token);
                    Long operatorId = jwtService.extractOperatorId(token);

                    var authorities = roles.stream()
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                            .toList();

                    var auth = new UsernamePasswordAuthenticationToken(email, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    // On stocke l'operatorId comme attribut de requete pour le controleur
                    if (operatorId != null) {
                        request.setAttribute("operatorId", operatorId);
                    }
                }
            } catch (Exception e) {
                LOGGER.debug("JWT invalide ignore: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
