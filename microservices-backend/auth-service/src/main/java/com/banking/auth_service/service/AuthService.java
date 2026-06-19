package com.banking.auth_service.service;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginRequest;
import com.banking.auth_service.dto.RegisterRequest;
import com.banking.auth_service.dto.UserResponse;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.Utilisateur;
import com.banking.auth_service.repository.UtilisateurRepository;
import com.banking.auth_service.security.JwtService;
import com.banking.auth_service.security.GoogleTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final RestClient customerRestClient;

    // INSCRIPTION : cree un utilisateur avec mot de passe hache + role CLIENT par defaut.
    public UserResponse register(RegisterRequest req) {
        if (utilisateurRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise: " + req.email());
        }
        Utilisateur u = Utilisateur.builder()
                .email(req.email())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(req.telephone())
                .roles(new HashSet<>(Set.of(Role.CLIENT)))
                .build();
        u = utilisateurRepository.save(u);

        // Cree automatiquement la fiche client dans customer-service
        creerFicheClient(u);

        return toResponse(u);
    }

    // Cree la fiche client dans customer-service avec des valeurs par defaut
    private void creerFicheClient(Utilisateur u) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("utilisateurId", u.getId().toString());
            body.put("operateurId", 1L);
            body.put("nom", u.getEmail().split("@")[0]);
            body.put("prenom", u.getEmail().split("@")[0]);
            body.put("dateNaissance", "1990-01-01");
            body.put("email", u.getEmail());
            body.put("telephone", u.getTelephone() != null ? u.getTelephone() : "");
            body.put("numeroIdentite", "");
            body.put("typePiece", "CNI");
            body.put("adresse", Map.of(
                    "rue", "",
                    "ville", "",
                    "pays", "Cameroun",
                    "codePostal", ""
            ));

            customerRestClient.post()
                    .uri("/api/customers")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Fiche client creee pour {}", u.getEmail());
        } catch (Exception e) {
            LOGGER.warn("Impossible de creer la fiche client pour {}: {}", u.getEmail(), e.getMessage());
        }
    }

    // CONNEXION : verifie les identifiants, renvoie un jeton JWT.
    public AuthResponse login(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (!passwordEncoder.matches(req.motDePasse(), u.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }

        String token = jwtService.generateToken(u);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }

    // Renvoie l'utilisateur courant (a partir de son email, extrait du jeton).
    public UserResponse me(String email) {
        return utilisateurRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    // CONNEXION GOOGLE : verifie le jeton Google, cree l'utilisateur s'il n'existe pas,
    // puis delivre NOTRE jeton JWT (meme format que le login classique).
    public AuthResponse googleLogin(String idToken) {
        String email = googleTokenVerifier.verifyAndGetEmail(idToken);
        Utilisateur u = utilisateurRepository.findByEmail(email).orElseGet(() -> {
            Utilisateur nouveau = Utilisateur.builder()
                    .email(email)
                    .motDePasse(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(new HashSet<>(Set.of(Role.CLIENT)))
                    .build();
            Utilisateur sauvegarde = utilisateurRepository.save(nouveau);
            creerFicheClient(sauvegarde);
            return sauvegarde;
        });
        return new AuthResponse(jwtService.generateToken(u), "Bearer", jwtService.getExpirationSeconds());
    }

    private UserResponse toResponse(Utilisateur u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRoles());
    }
}
