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
        utilisateurRepository.findAll().forEach(this::migrateLegacyRoles);

        Utilisateur admin = utilisateurRepository.findByEmailIgnoreCase("admin@bank.cm")
                .orElseGet(() -> Utilisateur.builder()
                    .email("admin@bank.cm")
                    .motDePasse(passwordEncoder.encode("admin123"))
                    .build());

        admin.setRoles(new HashSet<>(Set.of(Role.ADMIN_PLATFORM)));
        utilisateurRepository.save(admin);
    }

    private void migrateLegacyRoles(Utilisateur user) {
        Set<Role> roles = new HashSet<>(user.getRoles());
        boolean changed = false;

        if (roles.remove(Role.ADMIN)) {
            roles.add(Role.ADMIN_PLATFORM);
            changed = true;
        }
        if (user.getOperatorId() != null && roles.remove(Role.OPERATEUR)) {
            roles.add(Role.OPERATOR_AGENT);
            changed = true;
        }
        if (changed) {
            user.setRoles(roles);
            utilisateurRepository.save(user);
        }
    }
}
