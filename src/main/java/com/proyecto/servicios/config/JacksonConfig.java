package com.proyecto.servicios.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Clase de configuración para la serialización y deserialización JSON con Jackson.
 * 
 * <p>Esta configuración garantiza que:
 * <ul>
 *   <li>Los valores nulos no sean incluidos en las respuestas JSON ({@link JsonInclude.Include#NON_NULL}).</li>
 *   <li>El manejo de tipos de fecha y hora modernos (como {@link java.time.LocalDateTime}) sea tipado y no texto plano varchar.</li>
 *   <li>No se produzcan fallos ante propiedades desconocidas en el payload JSON.</li>
 * </ul>
 * </p>
 * 
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Configuration
public class JacksonConfig {

    /**
     * Define y personaliza el bean principal de {@link ObjectMapper} para la aplicación.
     *
     * @return una instancia configurada de {@link ObjectMapper}.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Registrar el módulo para soporte de tipos de fecha/hora de Java 8+ (JSR-310)
        mapper.registerModule(new JavaTimeModule());

        // Configuración para NO incluir atributos con valor null en la respuesta
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // Deshabilitar la escritura de fechas como números de timestamp para usar formato ISO-8601 estandarizado
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignorar propiedades desconocidas en la deserialización para mayor resiliencia ante cambios de contrato
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        return mapper;
    }
}
