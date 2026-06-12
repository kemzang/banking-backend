package com.banking.transaction_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI transactionServiceOpenApi() {
        return new OpenAPI().info(
                new Info()
                        .title("Transaction Service API")
                        .version("1.0.0")
                        .description(
                                "API de gestion des depots, retraits, transferts "
                                        + "et de leur historique."
                        )
                        .contact(new Contact().name("TP INF462"))
        );
    }
}
