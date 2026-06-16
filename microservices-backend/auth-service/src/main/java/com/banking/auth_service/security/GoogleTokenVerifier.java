package com.banking.auth_service.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;

/**
 * Verifie un jeton d'identite (ID token) emis par Google et en extrait l'email.
 * Le jeton vient du front (Google Identity Services) ; on valide sa signature
 * et son audience (notre client-id) avant de faire confiance a l'email.
 */
@Component
public class GoogleTokenVerifier {

    @Value("${app.google.client-id:}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    public String verifyAndGetEmail(String idTokenString) {
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
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton Google invalide.");
            }
            return idToken.getPayload().getEmail();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Verification du jeton Google echouee.");
        }
    }
}
