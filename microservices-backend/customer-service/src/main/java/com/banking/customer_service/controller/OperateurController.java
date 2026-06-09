package com.banking.customer_service.controller;

import com.banking.customer_service.dto.OperateurRequestDTO;
import com.banking.customer_service.dto.OperateurResponseDTO;
import com.banking.customer_service.service.OperateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operators")
@RequiredArgsConstructor
public class OperateurController {

    private final OperateurService operateurService;

    @PostMapping
    public ResponseEntity<OperateurResponseDTO> creer(@RequestBody OperateurRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(operateurService.creer(dto));
    }

    @GetMapping
    public ResponseEntity<List<OperateurResponseDTO>> lister() {
        return ResponseEntity.ok(operateurService.lister());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperateurResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(operateurService.getById(id));
    }
}
