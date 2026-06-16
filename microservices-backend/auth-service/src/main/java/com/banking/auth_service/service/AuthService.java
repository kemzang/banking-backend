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
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;   // BCrypt (defini dans SecurityConfig)
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;

    // INSCRIPTION : cree un utilisateur avec mot de passe hache + role CLIENT par defaut.
    public UserResponse register(RegisterRequest req) {
        if (utilisateurRepository.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise: " + req.email());
        }
        Utilisateur u = Utilisateur.builder()
                .email(req.email())
                .motDePasse(passwordEncoder.encode(req.motDePasse()))   // <-- HACHAGE ici
                .telephone(req.telephone())
                .roles(new HashSet<>(Set.of(Role.CLIENT)))
                .build();
        u = utilisateurRepository.save(u);
        return toResponse(u);
    }

    // CONNEXION : verifie les identifiants, renvoie un jeton JWT.
    public AuthResponse login(LoginRequest req) {
        Utilisateur u = utilisateurRepository.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides"));

        // matches() re-hache le mot de passe saisi et le compare au hachage stocke
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
                    // mot de passe aleatoire : l'utilisateur se connecte via Google, pas par mot de passe
                    .motDePasse(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .roles(new HashSet<>(Set.of(Role.CLIENT)))
                    .build();
            return utilisateurRepository.save(nouveau);
        });
        return new AuthResponse(jwtService.generateToken(u), "Bearer", jwtService.getExpirationSeconds());
    }

    private UserResponse toResponse(Utilisateur u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRoles());
    }
}
