package com.banking.transaction_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // Builder PAR DEFAUT (non load-balance) : utilise par le client Eureka pour
    // joindre l'annuaire en direct. Sans ce @Primary, Eureka capte le builder
    // @LoadBalanced et tente de resoudre "discovery-service" via le load-balancer
    // -> "No instances available" (blocage : il faut l'annuaire pour joindre l'annuaire).
    @Bean
    @Primary
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public RestClient accountRestClient(
            @LoadBalanced RestClient.Builder loadBalancedRestClientBuilder
    ) {
        return loadBalancedRestClientBuilder
                .baseUrl("http://account-service")
                .build();
    }
}
