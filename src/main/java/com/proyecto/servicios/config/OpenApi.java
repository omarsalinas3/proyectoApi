package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// CORRECCIÓN: Se agregó la anotación @Configuration faltante para que Spring registre correctamente el Bean de OpenAPI.
// CORRECCIÓN: Se parametrizó la URL del servidor para no dejarla fija con puerto 8081.

/**
 * Configuración de OpenAPI / Swagger para la documentación de los endpoints.
 */
@Configuration
public class OpenApi {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Servicios")
                        .description("Microservicio con integración a servicios externos")
                        .version("1.0.0"))
                .addServersItem(new Server().url("http://localhost:" + serverPort).description("Servidor Local"));
    }
}

