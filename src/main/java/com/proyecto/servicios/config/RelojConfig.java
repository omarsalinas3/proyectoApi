package com.proyecto.servicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Reloj de la aplicación. Inyectarlo (en lugar de usar {@code Instant.now()}) permite probar reglas que dependen
 * del tiempo, como la vigencia de los códigos 2FA o la espera mínima de 62 s antes de {@code confirmTx}.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Configuration
public class RelojConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
