package com.example.pharmacyservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pharmacyServiceOpenAPI() {
        final String bearerAuth = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Pharmacy Service API")
                        .description("Medicine inventory and stock management (Kafka saga consumer).")
                        .version("1.0.0")
                        .contact(new Contact().name("GrowBusiness RS")))
                .addSecurityItem(new SecurityRequirement().addList(bearerAuth))
                .components(new Components()
                        .addSecuritySchemes(bearerAuth, new SecurityScheme()
                                .name(bearerAuth)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
