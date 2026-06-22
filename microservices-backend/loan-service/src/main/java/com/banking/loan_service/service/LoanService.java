package com.banking.loan_service.service;

import com.banking.loan_service.client.AccountClient;
import com.banking.loan_service.client.DocumentClient;
import com.banking.loan_service.client.DocumentClient.DocumentAnalysisDTO;
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
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LoanService {

    private final DemandePretRepository demandePretRepository;
    private final PretRepository pretRepository;
    private final EcheanceRepository echeanceRepository;
    private final RemboursementRepository remboursementRepository;
    private final AccountClient accountClient;
    private final DocumentClient documentClient;

    public DemandePretResponseDTO soumettre(DemandePretRequestDTO request) {
        log.info("Soumission d'une demande de prêt pour le client {}", request.clientId());

        List<DocumentAnalysisDTO> documents = documentClient.getAnalysesByClient(request.clientId());
        log.info("Client {} a {} document(s) OCR analyse(s)", request.clientId(), documents.size());

        BigDecimal scoreRisque = calculerScoreRisque(
                request.montantDemande(), request.dureeMois(), documents);

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
        
        // Créditer le compte du client avec le montant du prêt
        try {
            accountClient.credit(decision.compteId(), demande.getMontantDemande(),
                    "Déblocage prêt #" + pret.getId());
            log.info("Compte {} crédité de {} pour le prêt #{}", decision.compteId(), demande.getMontantDemande(), pret.getId());
        } catch (Exception e) {
            log.warn("Le prêt a été approuvé mais le crédit du compte a échoué: {}", e.getMessage());
        }
        
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

    public List<DemandePretResponseDTO> getAllDemandes() {
        return demandePretRepository.findAll().stream()
                .map(d -> new DemandePretResponseDTO(
                        d.getId(),
                        d.getClientId(),
                        d.getMontantDemande(),
                        d.getDureeMois(),
                        d.getScoreRisque(),
                        d.getStatut()
                ))
                .toList();
    }

    public List<DemandePretResponseDTO> getDemandesByClientId(Long clientId) {
        return demandePretRepository.findByClientId(clientId).stream()
                .map(d -> new DemandePretResponseDTO(
                        d.getId(),
                        d.getClientId(),
                        d.getMontantDemande(),
                        d.getDureeMois(),
                        d.getScoreRisque(),
                        d.getStatut()
                ))
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

    public List<PretResponseDTO> getAllPrets() {
        return pretRepository.findAll().stream()
                .map(p -> new PretResponseDTO(
                        p.getId(),
                        p.getClientId(),
                        p.getMontantAccorde(),
                        p.getTauxInteret(),
                        p.getDureeMois(),
                        p.getCapitalRestant(),
                        p.getStatut()
                ))
                .toList();
    }

    public List<PretResponseDTO> getPretsByClientId(Long clientId) {
        return pretRepository.findByClientId(clientId).stream()
                .map(p -> new PretResponseDTO(
                        p.getId(),
                        p.getClientId(),
                        p.getMontantAccorde(),
                        p.getTauxInteret(),
                        p.getDureeMois(),
                        p.getCapitalRestant(),
                        p.getStatut()
                ))
                .toList();
    }

    private BigDecimal calculerScoreRisque(BigDecimal montant, int dureeMois,
                                           List<DocumentAnalysisDTO> documents) {
        BigDecimal facteurMontant = montant.divide(BigDecimal.valueOf(1000000), 4, RoundingMode.HALF_UP);
        BigDecimal facteurDuree = BigDecimal.valueOf(dureeMois).divide(BigDecimal.valueOf(60), 4, RoundingMode.HALF_UP);

        BigDecimal score = facteurMontant.multiply(BigDecimal.valueOf(0.4))
                .add(facteurDuree.multiply(BigDecimal.valueOf(0.3)));

        BigDecimal bonusOcr = calculerBonusOcr(documents);
        score = score.add(bonusOcr);

        return score.min(BigDecimal.ONE).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculerBonusOcr(List<DocumentAnalysisDTO> documents) {
        if (documents == null || documents.isEmpty()) {
            log.info("Aucun document OCR: pas de bonus, score de base");
            return BigDecimal.valueOf(0.3);
        }

        boolean hasSalaire = false;
        boolean hasCni = false;
        boolean hasReleve = false;
        BigDecimal salaireMensuel = BigDecimal.ZERO;

        for (DocumentAnalysisDTO doc : documents) {
            if (!"completed".equals(doc.status())) continue;

            if ("salaire".equals(doc.documentType())) {
                hasSalaire = true;
                Map<String, Object> fields = doc.structuredFields();
                if (fields != null && fields.containsKey("salaireMensuel")) {
                    try {
                        salaireMensuel = new BigDecimal(fields.get("salaireMensuel").toString());
                    } catch (NumberFormatException e) {
                        log.warn("Salaire mensuel non numerique: {}", fields.get("salaireMensuel"));
                    }
                }
            } else if ("cni".equals(doc.documentType())) {
                hasCni = true;
            } else if ("releve_bancaire".equals(doc.documentType())) {
                hasReleve = true;
            }
        }

        BigDecimal bonus = BigDecimal.ZERO;

        if (hasCni) {
            bonus = bonus.add(BigDecimal.valueOf(0.05));
            log.info("Bonus KYC (CNI): +0.05");
        }
        if (hasSalaire) {
            bonus = bonus.add(BigDecimal.valueOf(0.05));
            log.info("Bonus bulletin de salaire: +0.05");
        }
        if (hasReleve) {
            bonus = bonus.add(BigDecimal.valueOf(0.05));
            log.info("Bonus releve bancaire: +0.05");
        }

        if (salaireMensuel.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal mensualiteEstimee = BigDecimal.valueOf(100000).divide(
                    BigDecimal.valueOf(36), 2, RoundingMode.HALF_UP);
            BigDecimal ratioEndettement = mensualiteEstimee.divide(
                    salaireMensuel, 4, RoundingMode.HALF_UP);

            if (ratioEndettement.compareTo(BigDecimal.valueOf(0.33)) <= 0) {
                bonus = bonus.add(BigDecimal.valueOf(0.05));
                log.info("Ratio endettement favorable ({}): +0.05", ratioEndettement);
            }
        }

        int nbDocuments = (int) documents.stream()
                .filter(d -> "completed".equals(d.status()))
                .count();
        if (nbDocuments >= 3) {
            bonus = bonus.subtract(BigDecimal.valueOf(0.05));
            log.info("Dossier complet ({} documents): -0.05 sur le risque", nbDocuments);
        }

        return bonus.negate();
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