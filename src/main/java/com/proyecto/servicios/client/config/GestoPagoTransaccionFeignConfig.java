package com.proyecto.servicios.client.config;

import com.proyecto.servicios.client.decoder.ProductosErrorDecoder;
import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

/**
 * Configuración del cliente Feign de transacciones GestoPago.
 *
 * <ul>
 *   <li>El token NO se inyecta aquí: el flujo lo obtiene explícitamente (nodo «Obtener Token») y lo envía.</li>
 *   <li>Se agrega {@code X-API-Key} si fue proporcionada, como indican los Key Points.</li>
 *   <li>Tiempo de lectura de 62 s: después de ese límite la transacción se resuelve con confirmTx.</li>
 *   <li>Sin reintentos automáticos: reintentar un sendTx a ciegas podría duplicar un cobro.</li>
 * </ul>
 *
 * <p>No se anota con {@code @Configuration} para que solo aplique a este cliente.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class GestoPagoTransaccionFeignConfig {

    @Value("${gestopago.api-key:}")
    private String apiKey;

    @Value("${gestopago.transaccion.connect-timeout-ms:5000}")
    private int connectTimeout;

    @Value("${gestopago.transaccion.read-timeout-ms:62000}")
    private int readTimeout;

    @Bean
    public RequestInterceptor gestoPagoTransaccionInterceptor() {
        return template -> {
            if (apiKey != null && !apiKey.isBlank()) {
                template.header("X-API-Key", apiKey.trim());
            }
            template.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE,
                    MediaType.TEXT_PLAIN_VALUE, MediaType.APPLICATION_JSON_VALUE);
        };
    }

    @Bean
    public Request.Options gestoPagoTransaccionOptions() {
        return new Request.Options(connectTimeout, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, true);
    }

    @Bean
    public Retryer gestoPagoTransaccionRetryer() {
        return Retryer.NEVER_RETRY;
    }

    @Bean
    public ErrorDecoder gestoPagoTransaccionErrorDecoder() {
        return new ProductosErrorDecoder();
    }

    @Bean
    public Logger.Level gestoPagoTransaccionLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
