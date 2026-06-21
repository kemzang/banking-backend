package com.banking.auth_service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

@Component
public class GoogleTokenVerifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleTokenVerifier.class);

    @Value("${app.google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    public String verifyAndGetEmail(String idTokenString) {
        LOGGER.info("Google clientId configuré: {}", clientId);
        LOGGER.info("Token reçu (longueur): {}", idTokenString != null ? idTokenString.length() : "null");

        if (clientId == null || clientId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Connexion Google non configuree (variable GOOGLE_CLIENT_ID manquante).");
        }
        try {
            if (verifier == null) {
                verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                        .setAudience(Collections.singletonList(clientId))
                        .build();
            }
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                LOGGER.error("Jeton Google invalide - verification echouee pour clientId: {}", clientId);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton Google invalide.");
            }
            String email = idToken.getPayload().getEmail();
            LOGGER.info("Email extrait du token Google: {}", email);
            return email;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Erreur verification token Google: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification du jeton Google echouee.");
        }
    }
}
