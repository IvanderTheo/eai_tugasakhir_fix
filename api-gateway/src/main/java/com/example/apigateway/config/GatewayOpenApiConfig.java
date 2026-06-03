package com.example.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Configuration
@Profile("!test")
public class GatewayOpenApiConfig {

    private static final Logger logger = LoggerFactory.getLogger(GatewayOpenApiConfig.class);

    @Bean
    public OpenAPI gatewayOpenApiSpec() throws IOException {
        logger.info("Loading OpenAPI specification from yaml file...");
        
        ClassPathResource resource = new ClassPathResource("openapi/growbusiness-gateway-api.yaml");
        if (!resource.exists()) {
            resource = new ClassPathResource("static/openapi/growbusiness-gateway-api.yaml");
        }
        
        if (!resource.exists()) {
            logger.error("OpenAPI YAML file not found!");
            throw new IllegalStateException("OpenAPI YAML file not found in classpath");
        }
        
        String yaml = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        SwaggerParseResult result = new OpenAPIV3Parser().readContents(yaml, null, null);
        
        if (result.getOpenAPI() == null) {
            logger.error("Failed to parse OpenAPI YAML: {}", result.getMessages());
            throw new IllegalStateException("Failed to parse OpenAPI YAML: " + result.getMessages());
        }
        
        OpenAPI openAPI = result.getOpenAPI();
        
        // Ensure servers are set
        if (openAPI.getServers() == null || openAPI.getServers().isEmpty()) {
            Server server = new Server();
            server.setUrl("http://localhost:8000");
            server.setDescription("API Gateway (Local)");
            openAPI.setServers(Arrays.asList(server));
        }
        
        logger.info("OpenAPI specification loaded successfully");
        logger.info("Available paths: {}", openAPI.getPaths().keySet());
        
        return openAPI;
    }
}
