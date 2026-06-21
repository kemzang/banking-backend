package com.banking.transaction_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

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
    public RestClient customerRestClient() {
        return RestClient.builder()
                .baseUrl("http://customer-service:8081")
                .build();
    }
}
