package com.banking.loan_service.service;

import com.banking.loan_service.client.AccountClient;
import com.banking.loan_service.client.DocumentClient;
import com.banking.loan_service.dto.*;
import com.banking.loan_service.entity.*;
import com.banking.loan_service.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private DemandePretRepository demandePretRepository;

    @Mock
    private PretRepository pretRepository;

    @Mock
    private EcheanceRepository echeanceRepository;

    @Mock
    private RemboursementRepository remboursementRepository;

    @Mock
    private AccountClient accountClient;

    @Mock
    private DocumentClient documentClient;

    @InjectMocks
    private LoanService loanService;

    @Test
    void testSoumettreDemande() {
        // Given
        DemandePretRequestDTO request = new DemandePretRequestDTO(
                1L,
                BigDecimal.valueOf(1000000),
                12,
                "Achat véhicule"
        );

        DemandePret demandeSauvee = DemandePret.builder()
                .id(1L)
                .clientId(1L)
                .montantDemande(BigDecimal.valueOf(1000000))
                .dureeMois(12)
                .motif("Achat véhicule")
                .scoreRisque(BigDecimal.valueOf(0.30))
                .statut(StatutDemande.SOUMISE)
                .dateSoumission(LocalDateTime.now())
                .build();

        when(documentClient.getAnalysesByClient(1L)).thenReturn(Collections.emptyList());
        when(demandePretRepository.save(any(DemandePret.class))).thenReturn(demandeSauvee);

        // When
        DemandePretResponseDTO response = loanService.soumettre(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals(1L, response.clientId());
        assertEquals(BigDecimal.valueOf(1000000), response.montantDemande());
        assertEquals(12, response.dureeMois());
        assertEquals(StatutDemande.SOUMISE, response.statut());
        assertNotNull(response.scoreRisque());

        verify(documentClient, times(1)).getAnalysesByClient(1L);
        verify(demandePretRepository, times(1)).save(any(DemandePret.class));
    }

    @Test
    void testDeciderApprouver() {
        // Given
        Long demandeId = 1L;
        DecisionRequestDTO decision = new DecisionRequestDTO(
                true, 
                BigDecimal.valueOf(0.12), 
                1L, 
                null
        );
        
        DemandePret demande = DemandePret.builder()
                .id(1L)
                .clientId(1L)
                .montantDemande(BigDecimal.valueOf(1000000))
                .dureeMois(12)
                .statut(StatutDemande.SOUMISE)
                .build();

        Pret pretSauve = Pret.builder()
                .id(1L)
                .demandeId(1L)
                .clientId(1L)
                .compteId(1L)
                .montantAccorde(BigDecimal.valueOf(1000000))
                .tauxInteret(BigDecimal.valueOf(0.12))
                .dureeMois(12)
                .capitalRestant(BigDecimal.valueOf(1000000))
                .statut(StatutPret.ACTIF)
                .dateDeblocage(LocalDateTime.now())
                .build();

        when(demandePretRepository.findById(demandeId)).thenReturn(Optional.of(demande));
        when(demandePretRepository.save(any(DemandePret.class))).thenReturn(demande);
        when(pretRepository.save(any(Pret.class))).thenReturn(pretSauve);

        // When
        Object response = loanService.decider(demandeId, decision);

        // Then
        assertNotNull(response);
        assertInstanceOf(PretResponseDTO.class, response);
        
        PretResponseDTO pretResponse = (PretResponseDTO) response;
        assertEquals(1L, pretResponse.id());
        assertEquals(1L, pretResponse.clientId());
        assertEquals(BigDecimal.valueOf(1000000), pretResponse.montantAccorde());
        assertEquals(StatutPret.ACTIF, pretResponse.statut());
        
        verify(demandePretRepository, times(1)).findById(demandeId);
        verify(demandePretRepository, times(1)).save(any(DemandePret.class));
        verify(pretRepository, times(1)).save(any(Pret.class));
    }

    @Test
    void testDeciderRejeter() {
        // Given
        Long demandeId = 1L;
        DecisionRequestDTO decision = new DecisionRequestDTO(
                false, 
                null, 
                null, 
                "Score de risque trop élevé"
        );
        
        DemandePret demande = DemandePret.builder()
                .id(1L)
                .clientId(1L)
                .montantDemande(BigDecimal.valueOf(1000000))
                .dureeMois(12)
                .scoreRisque(BigDecimal.valueOf(0.80))
                .statut(StatutDemande.SOUMISE)
                .build();

        when(demandePretRepository.findById(demandeId)).thenReturn(Optional.of(demande));
        when(demandePretRepository.save(any(DemandePret.class))).thenReturn(demande);

        // When
        Object response = loanService.decider(demandeId, decision);

        // Then
        assertNotNull(response);
        assertInstanceOf(DemandePretResponseDTO.class, response);
        
        DemandePretResponseDTO demandeResponse = (DemandePretResponseDTO) response;
        assertEquals(StatutDemande.REJETEE, demandeResponse.statut());
        
        verify(demandePretRepository, times(1)).findById(demandeId);
        verify(demandePretRepository, times(1)).save(any(DemandePret.class));
        verify(pretRepository, never()).save(any(Pret.class));
    }
}