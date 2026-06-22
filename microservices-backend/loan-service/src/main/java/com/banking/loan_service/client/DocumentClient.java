package com.banking.loan_service.client;

import java.util.List;
import java.util.Map;

public interface DocumentClient {
    List<DocumentAnalysisDTO> getAnalysesByClient(Long clientId);

    record DocumentAnalysisDTO(
        Long id,
        String documentType,
        Map<String, Object> structuredFields,
        String status,
        String createdAt
    ) {}
}
