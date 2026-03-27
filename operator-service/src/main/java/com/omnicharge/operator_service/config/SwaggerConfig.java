package com.omnicharge.operator_service.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "OmniCharge - Operator Service API",
        version = "1.0",
        description = "Manages telecom operators and recharge plans. ADMIN-only write operations.",
        contact = @Contact(name = "OmniCharge Team", email = "support@omnicharge.com")
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Via API Gateway (Docker/Local)"),
        @Server(url = "http://localhost:8082", description = "Direct - Operator Service")
    },
    security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    in = SecuritySchemeIn.HEADER,
    description = "Enter your JWT token. Obtain it from POST /api/auth/login"
)
public class SwaggerConfig {
}