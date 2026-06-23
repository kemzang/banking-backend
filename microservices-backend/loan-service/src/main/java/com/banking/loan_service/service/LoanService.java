package com.banking.loan_service.service;

import com.banking.loan_service.dto.*;
import com.banking.loan_service.entity.*;
import com.banking.loan_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final DemandePretRepository demandePretRepository;
    private final PretRepository pretRepository;
    private final EcheanceRepository echeanceRepository;
    private final RemboursementRepository remboursementRepository;

    public DemandePretResponseDTO soumettre(DemandePretRequestDTO request) {
        log.info("Soumission d'une demande de prêt pour le client {}", request.clientId());
        
        // Calcul simple du score de risque (exemple: basé sur le montant et la durée)
        BigDecimal scoreRisque = calculerScoreRisque(request.montantDemande(), request.dureeMois());
        
        DemandePret demande = DemandePret.builder()
                .clientId(request.clientId())
                .montantDemande(request.montantDemande())
                .dureeMois(request.dureeMois())
                .motif(request.motif())
                .scoreRisque(scoreRisque)
                .build();
        
        demande = demandePretRepository.save(demande);
        
        return new DemandePretResponseDTO(
                demande.getId(),
                demande.getClientId(),
                demande.getMontantDemande(),
                demande.getDureeMois(),
                demande.getScoreRisque(),
                demande.getStatut()
        );
    }

    public Object decider(Long demandeId, DecisionRequestDTO decision) {
        log.info("Décision sur la demande de prêt {}: {}", demandeId, decision.approuver() ? "APPROUVE" : "REJETE");
        
        DemandePret demande = demandePretRepository.findById(demandeId)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée: " + demandeId));
        
        if (demande.getStatut() != StatutDemande.SOUMISE) {
            throw new RuntimeException("La demande a déjà été traitée");
        }
        
        if (!decision.approuver()) {
            // Rejeter la demande
            demande.setStatut(StatutDemande.REJETEE);
            demandePretRepository.save(demande);
            
            // TODO: Publier événement PretRejete
            log.info("Demande de prêt {} rejetée: {}", demandeId, decision.motifRejet());
            
            return new DemandePretResponseDTO(
                    demande.getId(),
                    demande.getClientId(),
                    demande.getMontantDemande(),
                    demande.getDureeMois(),
                    demande.getScoreRisque(),
                    demande.getStatut()
            );
        }
        
        // Approuver la demande
        demande.setStatut(StatutDemande.APPROUVEE);
        demandePretRepository.save(demande);
        
        // Créer le prêt
        Pret pret = Pret.builder()
                .demandeId(demande.getId())
                .clientId(demande.getClientId())
                .compteId(decision.compteId())
                .montantAccorde(demande.getMontantDemande())
                .tauxInteret(decision.tauxInteret())
                .dureeMois(demande.getDureeMois())
                .capitalRestant(demande.getMontantDemande())
                .build();
        
        pret = pretRepository.save(pret);
        
        // Générer l'échéancier
        genererEcheancier(pret);
        
        // TODO: Appeler account-service pour créditer le compte
        // TODO: Publier événement PretApprouve
        
        log.info("Prêt {} créé avec succès pour le client {}", pret.getId(), pret.getClientId());
        
        return new PretResponseDTO(
                pret.getId(),
                pret.getClientId(),
                pret.getMontantAccorde(),
                pret.getTauxInteret(),
                pret.getDureeMois(),
                pret.getCapitalRestant(),
                pret.getStatut()
        );
    }

    public EcheancierDTO getEcheancier(Long pretId) {
        log.info("Consultation de l'échéancier du prêt {}", pretId);
        
        Pret pret = pretRepository.findById(pretId)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé: " + pretId));
        
        List<Echeance> echeances = echeanceRepository.findByPretIdOrderByNumero(pretId);
        
        List<EcheanceDTO> echeanceDTOs = echeances.stream()
                .map(e -> new EcheanceDTO(
                        e.getNumero(),
                        e.getDateEcheance(),
                        e.getMontantCapital(),
                        e.getMontantInteret(),
                        e.getMontantTotal(),
                        e.getStatut()
                ))
                .toList();
        
        return new EcheancierDTO(pretId, echeanceDTOs);
    }

    public PretResponseDTO rembourser(Long pretId, RemboursementRequestDTO request) {
        log.info("Remboursement du prêt {}: montant {}", pretId, request.montant());
        
        Pret pret = pretRepository.findById(pretId)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé: " + pretId));
        
        // Trouver la prochaine échéance à payer
        Echeance prochaineEcheance = echeanceRepository
                .findFirstByPretIdAndStatutOrderByNumero(pretId, StatutEcheance.A_PAYER)
                .orElseThrow(() -> new RuntimeException("Aucune échéance à payer pour ce prêt"));
        
        // Vérifier que le montant correspond à l'échéance
        if (request.montant().compareTo(prochaineEcheance.getMontantTotal()) != 0) {
            throw new RuntimeException("Le montant ne correspond pas à l'échéance due: " + 
                    prochaineEcheance.getMontantTotal());
        }
        
        // Marquer l'échéance comme payée
        prochaineEcheance.setStatut(StatutEcheance.PAYEE);
        echeanceRepository.save(prochaineEcheance);
        
        // Diminuer le capital restant
        BigDecimal nouveauCapitalRestant = pret.getCapitalRestant()
                .subtract(prochaineEcheance.getMontantCapital());
        pret.setCapitalRestant(nouveauCapitalRestant);
        
        // Enregistrer le remboursement
        Remboursement remboursement = Remboursement.builder()
                .echeanceId(prochaineEcheance.getId())
                .montant(request.montant())
                .moyenPaiement(request.moyenPaiement())
                .build();
        
        remboursementRepository.save(remboursement);
        
        // Vérifier si toutes les échéances sont payées
        boolean toutesPayees = echeanceRepository.findByPretIdOrderByNumero(pretId)
                .stream()
                .allMatch(e -> e.getStatut() == StatutEcheance.PAYEE);
        
        if (toutesPayees) {
            pret.setStatut(StatutPret.SOLDE);
            log.info("Prêt {} entièrement remboursé", pretId);
        }
        
        pret = pretRepository.save(pret);
        
        return new PretResponseDTO(
                pret.getId(),
                pret.getClientId(),
                pret.getMontantAccorde(),
                pret.getTauxInteret(),
                pret.getDureeMois(),
                pret.getCapitalRestant(),
                pret.getStatut()
        );
    }

    public DemandePretResponseDTO getDemandePret(Long id) {
        DemandePret demande = demandePretRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demande de prêt non trouvée: " + id));
        
        return new DemandePretResponseDTO(
                demande.getId(),
                demande.getClientId(),
                demande.getMontantDemande(),
                demande.getDureeMois(),
                demande.getScoreRisque(),
                demande.getStatut()
        );
    }

    public List<DemandePretResponseDTO> listerDemandesClient(Long clientId) {
        return demandePretRepository.findByClientIdOrderByDateSoumissionDesc(clientId).stream()
                .map(demande -> new DemandePretResponseDTO(
                        demande.getId(), demande.getClientId(), demande.getMontantDemande(),
                        demande.getDureeMois(), demande.getScoreRisque(), demande.getStatut()))
                .toList();
    }

    public List<PretResponseDTO> listerPretsClient(Long clientId) {
        return pretRepository.findByClientIdOrderByDateDeblocageDesc(clientId).stream()
                .map(pret -> new PretResponseDTO(
                        pret.getId(), pret.getClientId(), pret.getMontantAccorde(),
                        pret.getTauxInteret(), pret.getDureeMois(), pret.getCapitalRestant(), pret.getStatut()))
                .toList();
    }

    public PretResponseDTO getPret(Long id) {
        Pret pret = pretRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prêt non trouvé: " + id));
        
        return new PretResponseDTO(
                pret.getId(),
                pret.getClientId(),
                pret.getMontantAccorde(),
                pret.getTauxInteret(),
                pret.getDureeMois(),
                pret.getCapitalRestant(),
                pret.getStatut()
        );
    }

    private BigDecimal calculerScoreRisque(BigDecimal montant, int dureeMois) {
        // Calcul simple: plus le montant est élevé et la durée longue, plus le risque augmente
        BigDecimal facteurMontant = montant.divide(BigDecimal.valueOf(1000000), 4, RoundingMode.HALF_UP);
        BigDecimal facteurDuree = BigDecimal.valueOf(dureeMois).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);
        
        BigDecimal score = facteurMontant.multiply(BigDecimal.valueOf(0.6))
                .add(facteurDuree.multiply(BigDecimal.valueOf(0.4)));
        
        // Limiter le score entre 0 et 1
        return score.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }
    private void genererEcheancier(Pret pret) {
        log.info("Génération de l'échéancier pour le prêt {}", pret.getId());
        
        BigDecimal capital = pret.getMontantAccorde();
        BigDecimal tauxAnnuel = pret.getTauxInteret();
        int dureeMois = pret.getDureeMois();
        LocalDate dateDeblocage = pret.getDateDeblocage().toLocalDate();
        
        List<Echeance> echeances = new ArrayList<>();
        
        // Calcul du taux mensuel
        BigDecimal tauxMensuel = tauxAnnuel.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        
        // Calcul de la mensualité (amortissement à mensualités constantes)
        BigDecimal mensualite;
        if (tauxMensuel.compareTo(BigDecimal.ZERO) > 0) {
            // Formule: M = C * t / (1 - (1 + t)^(-n))
            BigDecimal unPlusTaux = BigDecimal.ONE.add(tauxMensuel);
            BigDecimal denominateur = BigDecimal.ONE.subtract(
                    BigDecimal.ONE.divide(
                            unPlusTaux.pow(dureeMois), 
                            8, 
                            RoundingMode.HALF_UP
                    )
            );
            mensualite = capital.multiply(tauxMensuel).divide(denominateur, 2, RoundingMode.HALF_UP);
        } else {
            // Si taux = 0, mensualité = capital / durée
            mensualite = capital.divide(BigDecimal.valueOf(dureeMois), 2, RoundingMode.HALF_UP);
        }
        
        BigDecimal capitalRestant = capital;
        
        for (int i = 1; i <= dureeMois; i++) {
            // Calcul des intérêts sur le capital restant
            BigDecimal interetMois = capitalRestant.multiply(tauxMensuel)
                    .setScale(2, RoundingMode.HALF_UP);
            
            // Calcul de la part capitale
            BigDecimal capitalMois = mensualite.subtract(interetMois);
            
            // Ajustement pour la dernière échéance (éviter les arrondis)
            if (i == dureeMois) {
                capitalMois = capitalRestant;
                mensualite = capitalMois.add(interetMois);
            }
            
            // Date d'échéance = date de déblocage + i mois
            LocalDate dateEcheance = dateDeblocage.plusMonths(i);
            
            Echeance echeance = Echeance.builder()
                    .pret(pret)
                    .numero(i)
                    .dateEcheance(dateEcheance)
                    .montantCapital(capitalMois)
                    .montantInteret(interetMois)
                    .montantTotal(mensualite)
                    .statut(StatutEcheance.A_PAYER)
                    .build();
            
            echeances.add(echeance);
            
            // Diminuer le capital restant
            capitalRestant = capitalRestant.subtract(capitalMois);
        }
        
        // Sauvegarder toutes les échéances
        echeanceRepository.saveAll(echeances);
        
        log.info("Échéancier généré: {} échéances, mensualité de {}", echeances.size(), mensualite);
    }
}
