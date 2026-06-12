package com.banking.loan_service.integration;

import com.banking.loan_service.dto.*;
import com.banking.loan_service.entity.*;
import com.banking.loan_service.service.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LoanWorkflowTest {

    @Autowired
    private LoanService loanService;

    @Test
    void testWorkflowCompletPret() {
        // 1. Soumettre une demande de prêt
        DemandePretRequestDTO demande = new DemandePretRequestDTO(
                1L,
                BigDecimal.valueOf(1000000),
                12,
                "Achat véhicule professionnel"
        );
        
        DemandePretResponseDTO demandeResponse = loanService.soumettre(demande);
        
        assertNotNull(demandeResponse);
        assertEquals(StatutDemande.SOUMISE, demandeResponse.statut());
        assertNotNull(demandeResponse.scoreRisque());
        
        // 2. Approuver la demande
        DecisionRequestDTO decision = new DecisionRequestDTO(
                true,
                BigDecimal.valueOf(0.12), // 12% annuel
                1L, // compte de versement
                null
        );
        
        Object decisionResponse = loanService.decider(demandeResponse.id(), decision);
        assertInstanceOf(PretResponseDTO.class, decisionResponse);
        
        PretResponseDTO pretResponse = (PretResponseDTO) decisionResponse;
        assertEquals(StatutPret.ACTIF, pretResponse.statut());
        assertEquals(BigDecimal.valueOf(1000000), pretResponse.capitalRestant());
        
        // 3. Consulter l'échéancier
        EcheancierDTO echeancier = loanService.getEcheancier(pretResponse.id());
        
        assertNotNull(echeancier);
        assertEquals(12, echeancier.echeances().size());
        
        // Vérifier la première échéance
        EcheanceDTO premiereEcheance = echeancier.echeances().get(0);
        assertEquals(1, premiereEcheance.numero());
        assertEquals(StatutEcheance.A_PAYER, premiereEcheance.statut());
        assertTrue(premiereEcheance.montantTotal().compareTo(BigDecimal.ZERO) > 0);
        
        // 4. Effectuer un remboursement
        RemboursementRequestDTO remboursement = new RemboursementRequestDTO(
                premiereEcheance.montantTotal(),
                MoyenPaiement.COMPTE
        );
        
        PretResponseDTO pretApresRemboursement = loanService.rembourser(pretResponse.id(), remboursement);
        
        // Vérifier que le capital restant a diminué
        assertTrue(pretApresRemboursement.capitalRestant()
                .compareTo(pretResponse.capitalRestant()) < 0);
        
        // 5. Vérifier que l'échéance est marquée comme payée
        EcheancierDTO echeancierApresRemboursement = loanService.getEcheancier(pretResponse.id());
        EcheanceDTO premiereEcheanceApres = echeancierApresRemboursement.echeances().get(0);
        assertEquals(StatutEcheance.PAYEE, premiereEcheanceApres.statut());
    }

    @Test
    void testRejetDemande() {
        // 1. Soumettre une demande
        DemandePretRequestDTO demande = new DemandePretRequestDTO(
                2L,
                BigDecimal.valueOf(5000000), // Montant élevé
                60, // Durée longue
                "Investissement risqué"
        );
        
        DemandePretResponseDTO demandeResponse = loanService.soumettre(demande);
        
        // 2. Rejeter la demande
        DecisionRequestDTO decision = new DecisionRequestDTO(
                false,
                null,
                null,
                "Score de risque trop élevé"
        );
        
        Object decisionResponse = loanService.decider(demandeResponse.id(), decision);
        assertInstanceOf(DemandePretResponseDTO.class, decisionResponse);
        
        DemandePretResponseDTO demandeRejetee = (DemandePretResponseDTO) decisionResponse;
        assertEquals(StatutDemande.REJETEE, demandeRejetee.statut());
    }
}