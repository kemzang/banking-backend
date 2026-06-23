package com.banking.auth_service.service;

import com.banking.auth_service.dto.OperatorUserRequest;
import com.banking.auth_service.dto.AuthResponse;
import com.banking.auth_service.dto.LoginRequest;
import com.banking.auth_service.dto.LoginType;
import com.banking.auth_service.dto.UserResponse;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.Utilisateur;
import com.banking.auth_service.repository.UtilisateurRepository;
import com.banking.auth_service.security.GoogleTokenVerifier;
import com.banking.auth_service.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class AuthServiceOperatorUserTests {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthService authService;
    private MockRestServiceServer customerServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        customerServer = MockRestServiceServer.bindTo(builder).build();
        authService = new AuthService(
                utilisateurRepository,
                new BCryptPasswordEncoder(),
                jwtService,
                googleTokenVerifier,
                builder.baseUrl("http://customer-service").build()
        );
    }

    @Test
    void createsOperatorUserAfterValidatingOrganization() {
        customerServer.expect(once(), requestTo("http://customer-service/api/operators/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        when(utilisateurRepository.existsByEmailIgnoreCase("agent@expressunion.cm")).thenReturn(false);
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur user = invocation.getArgument(0);
            user.setId(UUID.randomUUID());
            return user;
        });

        UserResponse response = authService.createOperatorUser(new OperatorUserRequest(
                "Agent",
                "Express",
                "Agent@ExpressUnion.cm",
                "Password123@",
                Role.OPERATOR_AGENT,
                42L
        ));

        assertThat(response.email()).isEqualTo("agent@expressunion.cm");
        assertThat(response.operatorId()).isEqualTo(42L);
        assertThat(response.roles()).containsExactly(Role.OPERATOR_AGENT);
        customerServer.verify();
    }

    @Test
    void rejectsNonOperatorRoleBeforeCallingCustomerService() {
        assertThatThrownBy(() -> authService.createOperatorUser(new OperatorUserRequest(
                "Jean",
                "Client",
                "jean@example.cm",
                "Password123@",
                Role.CLIENT,
                42L
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("OPERATOR_ADMIN ou OPERATOR_AGENT");

        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void logsInOperatorFromOperatorPortalAndReturnsOperatorContext() {
        UUID userId = UUID.randomUUID();
        Utilisateur user = Utilisateur.builder()
                .id(userId)
                .email("agent@expressunion.cm")
                .motDePasse(new BCryptPasswordEncoder().encode("Password123@"))
                .roles(new HashSet<>(Set.of(Role.OPERATOR_AGENT)))
                .operatorId(42L)
                .build();
        when(utilisateurRepository.findByEmailIgnoreCase("agent@expressunion.cm"))
                .thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(
                "agent@expressunion.cm",
                "Password123@",
                LoginType.OPERATOR_LOGIN
        ));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.roles()).containsExactly(Role.OPERATOR_AGENT);
        assertThat(response.operatorId()).isEqualTo(42L);
    }

    @Test
    void rejectsOperatorCredentialsOnAdminPortal() {
        Utilisateur user = Utilisateur.builder()
                .id(UUID.randomUUID())
                .email("agent@expressunion.cm")
                .motDePasse(new BCryptPasswordEncoder().encode("Password123@"))
                .roles(new HashSet<>(Set.of(Role.OPERATOR_AGENT)))
                .operatorId(42L)
                .build();
        when(utilisateurRepository.findByEmailIgnoreCase("agent@expressunion.cm"))
                .thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(
                "agent@expressunion.cm",
                "Password123@",
                LoginType.ADMIN_LOGIN
        )))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Role incompatible");

        verify(jwtService, never()).generateToken(any());
    }
}
