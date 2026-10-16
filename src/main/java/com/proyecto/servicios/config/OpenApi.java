package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// CORRECCIÓN: Se agregó la anotación @Configuration faltante para que Spring registre correctamente el Bean de OpenAPI.
// CORRECCIÓN: Se parametrizó la URL del servidor para no dejarla fija con puerto 8081.

/**
 * Configuración de OpenAPI / Swagger para la documentación de los endpoints.
 */
@Configuration
public class OpenApi {

    private static final String ESQUEMA_SEGURIDAD = "bearerAuth";


    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Servicios")
                        .description("Microservicio con integración a servicios externos")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURIDAD, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token obtenido en POST /api/v1/auth/login (campo token)")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURIDAD))
                .addServersItem(new Server().url("/").description("Servidor actual (mismo host desde el que se abre Swagger)"));
    }
}

