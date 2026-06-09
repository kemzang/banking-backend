package com.banking.customer_service.service;

import com.banking.customer_service.dto.OperateurRequestDTO;
import com.banking.customer_service.dto.OperateurResponseDTO;
import com.banking.customer_service.entity.Operateur;
import com.banking.customer_service.repository.OperateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OperateurService {

    private final OperateurRepository operateurRepository;

    public OperateurResponseDTO creer(OperateurRequestDTO dto) {
        if (operateurRepository.existsByCode(dto.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Code operateur deja utilise: " + dto.code());
        }
        Operateur op = Operateur.builder()
                .nom(dto.nom())
                .type(dto.type())
                .code(dto.code())
                .build();
        return toResponse(operateurRepository.save(op));
    }

    public OperateurResponseDTO getById(Long id) {
        return operateurRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Operateur introuvable: " + id));
    }

    public List<OperateurResponseDTO> lister() {
        return operateurRepository.findAll().stream().map(this::toResponse).toList();
    }

    private OperateurResponseDTO toResponse(Operateur o) {
        return new OperateurResponseDTO(o.getId(), o.getNom(), o.getType(), o.getCode());
    }
}
