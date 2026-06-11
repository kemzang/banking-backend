package com.banking.account_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.banking.account_service.dto.CompteRequestDTO;
import com.banking.account_service.dto.CompteResponseDTO;
import com.banking.account_service.dto.MouvementDTO;
import com.banking.account_service.dto.SoldeResponseDTO;
import com.banking.account_service.entity.Compte;
import com.banking.account_service.entity.StatutCompte;
import com.banking.account_service.repository.CompteRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompteService {

    private final CompteRepository compteRepository;

    // ------------------------------------------------------------------ //
    //  OUVRIR un compte
    // ------------------------------------------------------------------ //
    @Transactional
    public CompteResponseDTO ouvrirCompte(CompteRequestDTO dto) {
        if (dto.clientId() == null || dto.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "clientId et type sont obligatoires");
        }

        Compte compte = Compte.builder()
                .clientId(dto.clientId())
                .operateurId(dto.operateurId())
                .numeroCompte(genererNumero(dto.operateurId()))
                .type(dto.type())
                .devise(dto.devise() != null ? dto.devise() : "XAF")
                .solde(BigDecimal.ZERO)
                .plafondJournalier(dto.plafondJournalier())
                .decouvertAutorise(dto.decouvertAutorise() != null
                        ? dto.decouvertAutorise() : BigDecimal.ZERO)
                .statut(StatutCompte.ACTIF)
                .build();

        return toResponse(compteRepository.save(compte));
    }

    // ------------------------------------------------------------------ //
    //  LIRE / LISTER
    // ------------------------------------------------------------------ //
    public CompteResponseDTO getCompte(Long id) {
        return toResponse(findOrThrow(id));
    }

    public List<CompteResponseDTO> listerParClient(Long clientId) {
        return compteRepository.findByClientId(clientId)
                .stream().map(this::toResponse).toList();
    }

    public List<CompteResponseDTO> listerTous() {
        return compteRepository.findAll().stream().map(this::toResponse).toList();
    }

    public SoldeResponseDTO getSolde(Long id) {
        Compte c = findOrThrow(id);
        return new SoldeResponseDTO(c.getId(), c.getNumeroCompte(), c.getSolde(), c.getDevise());
    }

    // ------------------------------------------------------------------ //
    //  SUSPENDRE / CLÔTURER
    // ------------------------------------------------------------------ //
    @Transactional
    public CompteResponseDTO suspendre(Long id) {
        Compte c = findOrThrow(id);
        if (c.getStatut() == StatutCompte.CLOTURE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compte déjà clôturé");
        }
        c.setStatut(StatutCompte.SUSPENDU);
        return toResponse(compteRepository.save(c));
    }

    @Transactional
    public CompteResponseDTO cloturer(Long id) {
        Compte c = findOrThrow(id);
        if (c.getSolde().compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de clôturer un compte dont le solde est non nul");
        }
        c.setStatut(StatutCompte.CLOTURE);
        c.setDateCloture(LocalDateTime.now());
        return toResponse(compteRepository.save(c));
    }

    // ------------------------------------------------------------------ //
    //  CRÉDIT (usage interne — appelé par transaction-service)
    // ------------------------------------------------------------------ //
    @Transactional
    public SoldeResponseDTO crediter(Long id, MouvementDTO dto) {
        validerMontant(dto.montant());
        Compte c = findActifOrThrow(id);
        c.setSolde(c.getSolde().add(dto.montant()));
        return toSolde(compteRepository.save(c));
    }

    // ------------------------------------------------------------------ //
    //  DÉBIT (usage interne — appelé par transaction-service)
    // ------------------------------------------------------------------ //
    @Transactional
    public SoldeResponseDTO debiter(Long id, MouvementDTO dto) {
        validerMontant(dto.montant());
        Compte c = findActifOrThrow(id);

        BigDecimal nouveauSolde = c.getSolde().subtract(dto.montant());

        // Invariant : solde ne peut pas descendre sous le découvert autorisé
        if (nouveauSolde.compareTo(c.getDecouvertAutorise().negate()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solde insuffisant (solde: " + c.getSolde()
                    + ", découvert autorisé: " + c.getDecouvertAutorise() + ")");
        }

        // Invariant : plafond journalier (simple vérification unitaire)
        if (c.getPlafondJournalier() != null
                && dto.montant().compareTo(c.getPlafondJournalier()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Montant dépasse le plafond journalier (" + c.getPlafondJournalier() + ")");
        }

        c.setSolde(nouveauSolde);
        return toSolde(compteRepository.save(c));
    }

    // ------------------------------------------------------------------ //
    //  Helpers privés
    // ------------------------------------------------------------------ //
    private Compte findOrThrow(Long id) {
        return compteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compte introuvable: " + id));
    }

    private Compte findActifOrThrow(Long id) {
        Compte c = findOrThrow(id);
        if (c.getStatut() != StatutCompte.ACTIF) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Opération impossible : compte " + c.getStatut());
        }
        return c;
    }

    private void validerMontant(BigDecimal montant) {
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le montant doit être strictement positif");
        }
    }

    /**
     * Génère un numéro de compte unique : CM-{operateurId:04d}-{seq:06d}
     * Ex : CM-0001-000042
     */
    private String genererNumero(Long operateurId) {
        long seq = compteRepository.count() + 1;
        String base = String.format("CM-%04d-%06d", operateurId != null ? operateurId : 0, seq);
        // Collision guard (très improbable mais possible en cas de charge)
        while (compteRepository.existsByNumeroCompte(base)) {
            seq++;
            base = String.format("CM-%04d-%06d", operateurId != null ? operateurId : 0, seq);
        }
        return base;
    }

    private CompteResponseDTO toResponse(Compte c) {
        return new CompteResponseDTO(
                c.getId(), c.getNumeroCompte(), c.getClientId(), c.getOperateurId(),
                c.getType(), c.getSolde(), c.getDevise(),
                c.getPlafondJournalier(), c.getDecouvertAutorise(),
                c.getStatut(), c.getDateOuverture()
        );
    }

    private SoldeResponseDTO toSolde(Compte c) {
        return new SoldeResponseDTO(c.getId(), c.getNumeroCompte(), c.getSolde(), c.getDevise());
    }
}
