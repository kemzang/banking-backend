package com.banking.loan_service.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class RestDocumentClient implements DocumentClient {

    private final RestClient documentRestClient;

    public RestDocumentClient(RestClient documentRestClient) {
        this.documentRestClient = documentRestClient;
    }

    @Override
    public List<DocumentAnalysisDTO> getAnalysesByClient(Long clientId) {
        try {
            ApiResponse response = documentRestClient.get()
                    .uri("/api/v1/ocr/analysis/client/{clientId}", clientId)
                    .retrieve()
                    .body(ApiResponse.class);

            if (response != null && response.data != null) {
                return response.data.stream()
                        .map(item -> new DocumentAnalysisDTO(
                                item.id,
                                item.documentType,
                                item.structuredFields,
                                item.status,
                                item.createdAt
                        ))
                        .toList();
            }
            return Collections.emptyList();
        } catch (RestClientResponseException e) {
            log.warn("Erreur lors de la recuperation des documents OCR pour le client {}: {}",
                    clientId, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.warn("Service OCR indisponible pour le client {}: {}", clientId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ApiResponse {
        public String status;
        public String message;
        public List<AnalysisItem> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AnalysisItem {
        public Long id;
        @JsonProperty("document_type")
        public String documentType;
        @JsonProperty("structured_fields")
        public Map<String, Object> structuredFields;
        public String status;
        @JsonProperty("created_at")
        public String createdAt;
    }
}
