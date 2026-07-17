package com.impactbudget.auth;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata + a global bearer-JWT security scheme, so Swagger UI has an "Authorize" box. */
@Configuration
class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI impactBudgetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Impact Budget API")
                        .version("v1")
                        .description("Categorize spending by impact (local/independent, sustainability) "
                                + "and track goals & budgets. Authenticate via /api/v1/auth, then send "
                                + "the token as a bearer credential."))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
