package com.banking.auth_service.config;

import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.Utilisateur;
import com.banking.auth_service.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Cree un compte ADMIN par defaut au demarrage (s'il n'existe pas) :
 *   email: admin@bank.cm  /  mot de passe: admin123
 * Permet de demontrer les espaces differencies par role.
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!utilisateurRepository.existsByEmail("admin@bank.cm")) {
            utilisateurRepository.save(Utilisateur.builder()
                    .email("admin@bank.cm")
                    .motDePasse(passwordEncoder.encode("admin123"))
                    .roles(new HashSet<>(Set.of(Role.ADMIN, Role.CLIENT)))
                    .build());
        }
    }
}
