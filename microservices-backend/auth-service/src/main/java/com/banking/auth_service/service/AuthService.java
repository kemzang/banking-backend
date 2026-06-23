package com.banking.auth_service.service;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginRequest;
import com.banking.auth_service.dto.LoginType;
import com.banking.auth_service.dto.OperatorUserRequest;
import com.banking.auth_service.dto.RegisterRequest;
import com.banking.auth_service.dto.UserResponse;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.StatutUtilisateur;
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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
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
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete d'inscription obligatoire");
        }
        validateCredentials(req.email(), req.motDePasse());
        String email = normalizeEmail(req.email());
        ensureEmailAvailable(email);

        Utilisateur u = Utilisateur.builder()
                .email(email)
                .nom(req.nom())
                .prenom(req.prenom())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(req.telephone())
                .roles(new HashSet<>(Set.of(Role.CLIENT)))
                .build();
        u = utilisateurRepository.save(u);

        // Cree automatiquement la fiche client dans customer-service
        creerFicheClient(u);

        return toResponse(u);
    }

    public UserResponse createOperatorUser(OperatorUserRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete utilisateur operateur obligatoire");
        }
        validateCredentials(req.email(), req.motDePasse());
        if (req.role() == null || !req.role().isOperatorRole()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le role doit etre OPERATOR_ADMIN ou OPERATOR_AGENT"
            );
        }
        if (req.operatorId() == null || req.operatorId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operatorId est obligatoire");
        }

        validateOperatorExists(req.operatorId());
        String email = normalizeEmail(req.email());
        ensureEmailAvailable(email);

        Utilisateur user = Utilisateur.builder()
                .email(email)
                .nom(req.nom())
                .prenom(req.prenom())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .roles(new HashSet<>(Set.of(req.role())))
                .operatorId(req.operatorId())
                .build();

        return toResponse(utilisateurRepository.save(user));
    }

    // Cree la fiche client dans customer-service avec des valeurs par defaut
    private void creerFicheClient(Utilisateur u) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("utilisateurId", u.getId().toString());
            body.put("nom", valueOrDefault(u.getNom(), u.getEmail().split("@")[0]));
            body.put("prenom", valueOrDefault(u.getPrenom(), u.getEmail().split("@")[0]));
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
        if (req == null || req.email() == null || req.motDePasse() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email et mot de passe obligatoires");
        }
        Utilisateur u = utilisateurRepository.findByEmailIgnoreCase(normalizeEmail(req.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        if (!passwordEncoder.matches(req.motDePasse(), u.getMotDePasse())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides");
        }
        if (u.getStatut() == StatutUtilisateur.SUSPENDU) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Compte suspendu");
        }
        validateLoginType(u, req.loginType());

        String token = jwtService.generateToken(u);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds(), toResponse(u));
    }

    // Renvoie l'utilisateur courant (a partir de son email, extrait du jeton).
    public UserResponse me(String email) {
        return utilisateurRepository.findByEmailIgnoreCase(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
    }

    // CONNEXION GOOGLE : verifie le jeton Google, cree l'utilisateur s'il n'existe pas,
    // puis delivre NOTRE jeton JWT (meme format que le login classique).
    public AuthResponse googleLogin(String idToken) {
        String email = googleTokenVerifier.verifyAndGetEmail(idToken);
        Utilisateur u = utilisateurRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            Utilisateur nouveau = Utilisateur.builder()
                    .email(email)
                    .motDePasse(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(new HashSet<>(Set.of(Role.CLIENT)))
                    .build();
            Utilisateur sauvegarde = utilisateurRepository.save(nouveau);
            creerFicheClient(sauvegarde);
            return sauvegarde;
        });
        return new AuthResponse(jwtService.generateToken(u), "Bearer", jwtService.getExpirationSeconds(), toResponse(u));
    }

    private UserResponse toResponse(Utilisateur u) {
        return new UserResponse(
                u.getId(),
                u.getEmail(),
                Set.copyOf(u.getRoles()),
                u.getOperatorId(),
                u.getPrenom(),
                u.getNom()
        );
    }

    private void validateOperatorExists(Long operatorId) {
        try {
            customerRestClient.get()
                    .uri("/api/operators/{id}", operatorId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operateur introuvable: " + operatorId);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Validation de l'operateur impossible");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "customer-service indisponible");
        }
    }

    private void validateCredentials(String email, String password) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email invalide");
        }
        if (password == null || password.length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le mot de passe doit contenir au moins 8 caracteres");
        }
    }

    private void validateLoginType(Utilisateur user, LoginType loginType) {
        if (loginType == null) {
            return;
        }

        boolean authorized = switch (loginType) {
            case CLIENT_LOGIN -> user.getRoles().contains(Role.CLIENT);
            case ADMIN_LOGIN -> user.getRoles().contains(Role.ADMIN_PLATFORM);
            case OPERATOR_LOGIN -> user.getRoles().stream().anyMatch(Role::isOperatorRole);
        };
        if (!authorized) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Role incompatible avec la page de connexion");
        }
    }

    private void ensureEmailAvailable(String email) {
        if (utilisateurRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise: " + email);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
