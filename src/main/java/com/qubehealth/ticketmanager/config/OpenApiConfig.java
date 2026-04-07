package com.qubehealth.ticketmanager.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketManagerOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Ticket Manager API")
                        .description("API documentation for Ticket Management System")
                        .version("v1.0"));
    }
}
