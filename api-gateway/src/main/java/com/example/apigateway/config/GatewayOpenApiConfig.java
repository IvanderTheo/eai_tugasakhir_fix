package com.example.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@Profile("!test")
public class GatewayOpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenApiSpec() throws IOException {
        ClassPathResource resource = new ClassPathResource("openapi/growbusiness-gateway-api.yaml");
        if (!resource.exists()) {
            resource = new ClassPathResource("static/openapi/growbusiness-gateway-api.yaml");
        }
        String yaml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        if (result.getOpenAPI() == null) {
            throw new IllegalStateException("Failed to parse OpenAPI YAML: " + result.getMessages());
        }
        return result.getOpenAPI();
    }
}
