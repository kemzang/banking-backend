package com.banking.customer_service.service;

import com.banking.customer_service.dto.ClientRequestDTO;
import com.banking.customer_service.dto.ClientResponseDTO;
import com.banking.customer_service.entity.Client;
import com.banking.customer_service.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import main.java.com.banking.customer_service.entity.StatutKyc;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service                       // = composant métier géré par Spring
@RequiredArgsConstructor       // Lombok génère le constructeur pour les champs final
public class ClientService {

    private final ClientRepository clientRepository;

    // CRÉER un client
    public ClientResponseDTO creerClient(ClientRequestDTO dto) {
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
            return toResponse(clientSauvegarde);
        
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

    // --- conversion Entity -> DTO de sortie ---
    private ClientResponseDTO toResponse(Client c) {
        // return new ClientResponseDTO(c.getId(), c.getNom(), ...);
        return new ClientResponseDTO(
                c.getId(),
                c.getNom(),
                c.getPrenom(),
                c.getEmail(),
                c.getStatutKyc(),
                c.getOperateurId()
        );
    }
}
