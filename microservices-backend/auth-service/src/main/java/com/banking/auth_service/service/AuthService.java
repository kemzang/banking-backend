package com.banking.auth_service.service;

import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginRequest;
import com.banking.auth_service.dto.RegisterRequest;
import com.banking.auth_service.dto.UserResponse;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.Utilisateur;
import com.banking.auth_service.repository.UtilisateurRepository;
import com.banking.auth_service.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;   // BCrypt (defini dans SecurityConfig)
    private final JwtService jwtService;

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

    private UserResponse toResponse(Utilisateur u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRoles());
    }
}
