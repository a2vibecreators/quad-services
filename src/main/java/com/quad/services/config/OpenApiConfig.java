package com.quad.services.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) Configuration
 *
 * Provides API documentation at /swagger-ui.html
 * OpenAPI JSON spec available at /v3/api-docs
 *
 * @author QUAD Platform
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quadPlatformAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("QUAD Platform API")
                        .description("Quick Unified Agentic Development Platform - Backend API\n\n" +
                                "## Authentication\n" +
                                "Most endpoints require JWT authentication. Include the token in the Authorization header:\n" +
                                "```\n" +
                                "Authorization: Bearer <your-jwt-token>\n" +
                                "```\n\n" +
                                "## Getting Started\n" +
                                "1. Sign up using `/auth/signup`\n" +
                                "2. Login using `/auth/login` to get a JWT token\n" +
                                "3. Use the token for authenticated endpoints\n\n" +
                                "## Endpoints\n" +
                                "- **Authentication**: /auth/* - Signup, login, token management\n" +
                                "- **User Management**: /users/* - User profiles and lookups\n" +
                                "- **Health**: /health - Service health check")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("QUAD Platform Team")
                                .email("support@quadframe.work")
                                .url("https://quadframe.work"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://quadframe.work/license")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT token obtained from /auth/login or /auth/signup")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
