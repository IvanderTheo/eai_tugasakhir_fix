package com.example.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .description("""
                                Routes requests to microservices. **Swagger UI per service:**
                                
                                | Service | Swagger UI |
                                |---------|------------|
                                | Admin (auth + patients) | http://localhost:8001/swagger-ui.html |
                                | Medical | http://localhost:8002/swagger-ui.html |
                                | Payment | http://localhost:8003/swagger-ui.html |
                                | Pharmacy | http://localhost:8004/swagger-ui.html |
                                
                                **Gateway paths:** `/api/auth/**`, `/api/admin/**`, `/api/medical/**`, `/api/pharmacy/**`, `/api/payment/**`
                                
                                1. Login: `POST /api/auth/login` with JSON `{"username":"admin","password":"admin123"}`
                                2. Copy `token` and use **Authorize** on each service Swagger UI.
                                """)
                        .version("1.0.0")
                        .contact(new Contact().name("GrowBusiness RS")))
                .servers(List.of(new Server().url("http://localhost:8000").description("API Gateway")));
    }
}
