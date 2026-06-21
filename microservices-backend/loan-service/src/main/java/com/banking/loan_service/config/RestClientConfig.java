package com.banking.loan_service.config;

import com.banking.loan_service.client.AccountClient;
import com.banking.loan_service.client.RestAccountClient;
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
    public AccountClient accountClient(RestClient accountRestClient) {
        return new RestAccountClient(accountRestClient);
    }
}
