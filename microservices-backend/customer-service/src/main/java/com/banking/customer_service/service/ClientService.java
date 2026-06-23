package com.banking.customer_service.service;

import com.banking.customer_service.dto.ClientRequestDTO;
import com.banking.customer_service.dto.ClientResponseDTO;
import com.banking.customer_service.entity.Client;
import com.banking.customer_service.repository.ClientRepository;
import com.banking.customer_service.repository.OperateurRepository;
import com.banking.customer_service.entity.StatutOperateur;
import lombok.RequiredArgsConstructor;
import com.banking.customer_service.entity.StatutKyc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Service                       // = composant métier géré par Spring
@RequiredArgsConstructor       // Lombok génère le constructeur pour les champs final
public class ClientService {

    private final ClientRepository clientRepository;
    private final OperateurRepository operateurRepository;
    private final ClientWorkflowClient workflowClient;

    // CRÉER un client
    public ClientResponseDTO creerClient(ClientRequestDTO dto) {
        if (dto.operateurId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "operateurId est obligatoire");
        }
        var operator = operateurRepository.findById(dto.operateurId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operateur introuvable"));
        if (operator.getStatut() != StatutOperateur.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Operateur inactif");
        }
        // 1. si l'email existe déjà -> lever ResponseStatusException(HttpStatus.CONFLICT, "...")
        if(clientRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé: " + dto.email());
        }
        // 2. construire un Client à partir du dto (Client.builder()....build())
            Client client = Client.builder()
                    .utilisateurId(dto.utilisateurId())
                    .operateurId(dto.operateurId())
                    .nom(dto.nom())
                    .prenom(dto.prenom())
                    .dateNaissance(dto.dateNaissance())
                    .email(dto.email())
                    .telephone(dto.telephone())
                    .numeroIdentite(dto.numeroIdentite())
                    .typePiece(dto.typePiece())
                    .adresse(dto.adresse())
                    .statutKyc(StatutKyc.EN_ATTENTE) // statut initial
                    .build();
        // 3. clientRepository.save(client)
            Client clientSauvegarde = clientRepository.save(client);
        // 4. retourner toResponse(clientSauvegardé)
            ClientResponseDTO response = toResponse(clientSauvegarde);
            workflowClient.notifyOperator(response);
            return response;
        
    }

    // LIRE un client par id
    public ClientResponseDTO getClient(Long id) {
        // clientRepository.findById(id)
            return clientRepository.findById(id)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));
        //   .map(this::toResponse)
        //   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));
    }

    // LISTER tous les clients
    public List<ClientResponseDTO> lister() {
        return clientRepository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ClientResponseDTO> listerParOperateur(Long operatorId) {
        return clientRepository.findByOperateurId(operatorId).stream().map(this::toResponse).toList();
    }

    public List<ClientResponseDTO> listerEnAttente(Long operatorId, boolean global) {
        List<Client> clients = global
                ? clientRepository.findByStatutKyc(StatutKyc.EN_ATTENTE)
                : clientRepository.findByOperateurIdAndStatutKyc(operatorId, StatutKyc.EN_ATTENTE);
        return clients.stream().map(this::toResponse).toList();
    }

    @Transactional
    public ClientResponseDTO approuver(Long id) {
        Client client = findEntity(id);
        client.setStatutKyc(StatutKyc.VALIDE);
        client.setMotifRejet(null);
        if (client.getUtilisateurId() != null) workflowClient.updateUserStatus(client.getUtilisateurId(), "ACTIF");
        ClientResponseDTO response = toResponse(clientRepository.save(client));
        workflowClient.notifyClient(response, "CLIENT_APPROVED", "Votre compte a ete active.");
        return response;
    }

    @Transactional
    public ClientResponseDTO rejeter(Long id, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le motif de rejet est obligatoire");
        }
        Client client = findEntity(id);
        client.setStatutKyc(StatutKyc.REJETE);
        client.setMotifRejet(reason.trim());
        if (client.getUtilisateurId() != null) workflowClient.updateUserStatus(client.getUtilisateurId(), "REJETE");
        ClientResponseDTO response = toResponse(clientRepository.save(client));
        workflowClient.notifyClient(response, "CLIENT_REJECTED", "Votre compte a ete rejete. Motif : " + reason.trim());
        return response;
    }

    // RECUPERER un client par email
    public ClientResponseDTO getClientParEmail(String email) {
        return clientRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable pour cet email: " + email));
    }

    // MODIFIER un client existant (champs autorises ; ni id, ni statutKyc, ni dateInscription)
    public ClientResponseDTO modifier(Long id, ClientRequestDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));

        // si l'email change, verifier qu'il n'est pas deja pris par un autre client
        if (!client.getEmail().equals(dto.email()) && clientRepository.existsByEmail(dto.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email deja utilise: " + dto.email());
        }

        client.setOperateurId(dto.operateurId());
        client.setNom(dto.nom());
        client.setPrenom(dto.prenom());
        client.setDateNaissance(dto.dateNaissance());
        client.setEmail(dto.email());
        client.setTelephone(dto.telephone());
        client.setNumeroIdentite(dto.numeroIdentite());
        client.setTypePiece(dto.typePiece());
        client.setAdresse(dto.adresse());

        return toResponse(clientRepository.save(client));
    }

    public ClientResponseDTO modifierInformationsPersonnelles(Long id, ClientRequestDTO dto) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));
        client.setNom(dto.nom());
        client.setPrenom(dto.prenom());
        client.setDateNaissance(dto.dateNaissance());
        client.setTelephone(dto.telephone());
        client.setNumeroIdentite(dto.numeroIdentite());
        client.setTypePiece(dto.typePiece());
        client.setAdresse(dto.adresse());
        return toResponse(clientRepository.save(client));
    }

    // METTRE A JOUR le statut KYC
    public ClientResponseDTO majKyc(Long id, StatutKyc statut) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));
        client.setStatutKyc(statut);
        return toResponse(clientRepository.save(client));
    }

    private Client findEntity(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable: " + id));
    }

    // --- conversion Entity -> DTO de sortie ---
    private ClientResponseDTO toResponse(Client c) {
        // return new ClientResponseDTO(c.getId(), c.getNom(), ...);
        return new ClientResponseDTO(
                c.getId(),
                c.getNom(),
                c.getPrenom(),
                c.getEmail(),
                c.getStatutKyc(),
                c.getOperateurId(),
                c.getMotifRejet()
        );
    }
}
