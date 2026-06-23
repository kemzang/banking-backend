package com.banking.auth_service.service;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginRequest;
import com.banking.auth_service.dto.LoginType;
import com.banking.auth_service.dto.OperatorUserRequest;
import com.banking.auth_service.dto.OperatorAdminRequest;
import com.banking.auth_service.dto.OperatorAgentRequest;
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
import org.springframework.transaction.annotation.Transactional;
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
import java.util.List;
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
    @Transactional
    public UserResponse register(RegisterRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete d'inscription obligatoire");
        }
        validateCredentials(req.email(), req.motDePasse());
        validateOperatorId(req.operatorId());
        validateActiveOperator(req.operatorId());
        String email = normalizeEmail(req.email());
        ensureEmailAvailable(email);

        Utilisateur u = Utilisateur.builder()
                .email(email)
                .nom(req.nom())
                .prenom(req.prenom())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .telephone(req.telephone())
                .operatorId(req.operatorId())
                .statut(StatutUtilisateur.EN_ATTENTE)
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
        if (req.role() != Role.OPERATOR_ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ADMIN_PLATFORM ne peut creer que le premier OPERATOR_ADMIN"
            );
        }
        return createOperatorAdmin(new OperatorAdminRequest(
                req.prenom(), req.nom(), req.email(), req.motDePasse(), req.operatorId()
        ));
    }

    public UserResponse createOperatorAdmin(OperatorAdminRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete administrateur operateur obligatoire");
        }
        validateCredentials(req.email(), req.motDePasse());
        validateOperatorId(req.operatorId());
        validateOperatorExists(req.operatorId());
        if (utilisateurRepository.existsByOperatorIdAndRolesContaining(req.operatorId(), Role.OPERATOR_ADMIN)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un OPERATOR_ADMIN existe deja pour cet operateur"
            );
        }
        return createOperatorIdentity(
                req.prenom(), req.nom(), req.email(), req.motDePasse(),
                Role.OPERATOR_ADMIN, req.operatorId()
        );
    }

    public UserResponse createOperatorAgent(OperatorAgentRequest req, String creatorEmail) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requete agent operateur obligatoire");
        }
        validateCredentials(req.email(), req.motDePasse());
        Utilisateur creator = requireOperatorAdmin(creatorEmail);
        return createOperatorIdentity(
                req.prenom(), req.nom(), req.email(), req.motDePasse(),
                Role.OPERATOR_AGENT, creator.getOperatorId()
        );
    }

    public List<UserResponse> listOperatorAgents(String creatorEmail) {
        Utilisateur creator = requireOperatorAdmin(creatorEmail);
        return utilisateurRepository
                .findByOperatorIdAndRolesContaining(creator.getOperatorId(), Role.OPERATOR_AGENT)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UserResponse createOperatorIdentity(
            String firstName,
            String lastName,
            String rawEmail,
            String rawPassword,
            Role role,
            Long operatorId) {
        String email = normalizeEmail(rawEmail);
        ensureEmailAvailable(email);
        Utilisateur user = Utilisateur.builder()
                .email(email)
                .nom(lastName)
                .prenom(firstName)
                .motDePasse(passwordEncoder.encode(rawPassword))
                .roles(new HashSet<>(Set.of(role)))
                .operatorId(operatorId)
                .build();
        return toResponse(utilisateurRepository.save(user));
    }

    private Utilisateur requireOperatorAdmin(String email) {
        Utilisateur creator = utilisateurRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur courant introuvable"));
        if (!creator.getRoles().contains(Role.OPERATOR_ADMIN) || creator.getOperatorId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Creation reservee a OPERATOR_ADMIN");
        }
        return creator;
    }

    private void validateOperatorId(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operatorId est obligatoire");
        }
    }

    // Cree la fiche client dans customer-service avec des valeurs par defaut
    private void creerFicheClient(Utilisateur u) {
        if (u.getOperatorId() == null) {
            LOGGER.warn("Profil client differe pour {}: operateur non selectionne", u.getEmail());
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("utilisateurId", u.getId().toString());
            body.put("operateurId", u.getOperatorId());
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
                    .header("X-Internal-Service", "auth-service")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            LOGGER.info("Fiche client creee pour {}", u.getEmail());
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Impossible de creer le profil client",
                    e
            );
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
        if (u.getStatut() == StatutUtilisateur.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.LOCKED, "Compte en attente de validation par l'operateur");
        }
        if (u.getStatut() == StatutUtilisateur.REJETE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inscription rejetee par l'operateur");
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
                u.getNom(),
                u.getStatut()
        );
    }

    @Transactional
    public UserResponse updateInternalStatus(UUID userId, StatutUtilisateur status) {
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status est obligatoire");
        }
        Utilisateur user = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable"));
        user.setStatut(status);
        return toResponse(utilisateurRepository.save(user));
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

    private void validateActiveOperator(Long operatorId) {
        try {
            OperatorSnapshot operator = customerRestClient.get()
                    .uri("/api/operators/{id}", operatorId)
                    .retrieve()
                    .body(OperatorSnapshot.class);
            if (operator == null || !"ACTIVE".equals(operator.statut())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operateur inactif");
            }
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operateur introuvable: " + operatorId);
        } catch (ResponseStatusException e) {
            throw e;
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "customer-service indisponible");
        }
    }

    private record OperatorSnapshot(Long id, String statut) {}

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
            case CLIENT_LOGIN, CLIENT -> user.getRoles().contains(Role.CLIENT);
            case ADMIN_LOGIN, ADMIN -> user.getRoles().contains(Role.ADMIN_PLATFORM);
            case OPERATOR_LOGIN, OPERATOR -> user.getRoles().stream().anyMatch(Role::isOperatorRole);
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
