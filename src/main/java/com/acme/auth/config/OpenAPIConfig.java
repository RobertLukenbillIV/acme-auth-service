package com.acme.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Acme Auth Service API")
                        .description("Authentication and authorization service for the Acme platform. " +
                                "Provides secure signup, login, token refresh, and user management endpoints. " +
                                "Uses shared schemas and error models from acme-contracts for consistent responses.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Acme Platform")
                                .url("https://github.com/RobertLukenbillIV")
                        )
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")
                        )
                )
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.acme.com").description("Production")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token authentication. Obtain token via /api/auth/login or /api/auth/signup")
                        )
                )
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
