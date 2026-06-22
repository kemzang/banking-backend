package com.banking.loan_service.config;

import com.banking.loan_service.client.AccountClient;
import com.banking.loan_service.client.DocumentClient;
import com.banking.loan_service.client.RestAccountClient;
import com.banking.loan_service.client.RestDocumentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Value("${ai-document-service.url:http://ai-document-service:8001}")
    private String aiDocumentServiceUrl;

    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient accountRestClient() {
        return RestClient.builder()
                .baseUrl("http://account-service:8082")
                .build();
    }

    @Bean
    public AccountClient accountClient(RestClient accountRestClient) {
        return new RestAccountClient(accountRestClient);
    }

    @Bean
    public RestClient documentRestClient() {
        return RestClient.builder()
                .baseUrl(aiDocumentServiceUrl)
                .build();
    }

    @Bean
    public DocumentClient documentClient(RestClient documentRestClient) {
        return new RestDocumentClient(documentRestClient);
    }
}
