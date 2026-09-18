package com.proyecto.servicios.client.config;

import feign.Logger;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.concurrent.TimeUnit;

/**
 * Clase de configuración para el cliente Feign de integración con el servicio externo de productos.
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Inyectar de forma segura el Bearer Token configurado en el encabezado {@code Authorization}.</li>
 *   <li>Establecer los tiempos límite de conexión ({@code connectTimeout}) y de lectura ({@code readTimeout}).</li>
 *   <li>Configurar el nivel de detalle de logging de Feign para auditoría sin exponer tokens completos.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
public class ProductosFeignConfig {

    @Value("${servicio.externo.productos.token:}")
    private String fallbackBearerToken;

    @Value("${servicio.externo.productos.api-key:}")
    private String apiKey;

    @Value("${gestopago.auth.id-distribuidor:83}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo:GPS83-TPV-17}")
    private String codigoDispositivo;

    @Value("${servicio.externo.productos.connect-timeout:5000}")
    private int connectTimeout;

    @Value("${servicio.externo.productos.read-timeout:62000}")
    private int readTimeout;

    /**
     * Interceptor que agrega automáticamente el encabezado de autenticación Bearer Token,
     * obteniendo el token activo dinámico de {@link com.proyecto.servicios.service.GestoPagoTokenService}
     * o recurriendo al token de configuración como fallback.
     *
     * @param tokenServiceProvider proveedor perezoso del servicio de tokens de GestoPago.
     * @return instancia de {@link RequestInterceptor}.
     */
    @Bean
    public RequestInterceptor bearerAuthRequestInterceptor(org.springframework.beans.factory.ObjectProvider<com.proyecto.servicios.service.GestoPagoTokenService> tokenServiceProvider) {
        return (RequestTemplate template) -> {
            String token = null;

            // Intentar obtener el token activo dinámico recién renovado
            com.proyecto.servicios.service.GestoPagoTokenService tokenService = tokenServiceProvider.getIfAvailable();
            if (tokenService != null) {
                token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                        .map(com.proyecto.servicios.entity.gestopago.GestoPagoToken::getToken)
                        .orElse(null);
            }

            // Fallback al token estático de properties si no hay token en base de datos
            if (token == null || token.isBlank()) {
                token = fallbackBearerToken;
            }

            if (token != null && !token.isBlank()) {
                template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token.trim());
            } else {
                log.warn("El Bearer Token para el servicio de productos no se encuentra disponible.");
            }

            if (apiKey != null && !apiKey.isBlank()) {
                template.header("X-API-Key", apiKey.trim());
            }

            template.header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE);
        };
    }



    /**
     * Configuración de los tiempos límite (timeouts) de conexión y lectura para el cliente Feign.
     *
     * @return instancia de {@link Request.Options} con los valores configurados.
     */
    @Bean
    public Request.Options feignRequestOptions() {
        return new Request.Options(
                connectTimeout, TimeUnit.MILLISECONDS,
                readTimeout, TimeUnit.MILLISECONDS,
                true // Seguir redirecciones
        );
    }

    /**
     * Registra el decodificador de errores personalizado para traducir respuestas no exitosas de Feign.
     *
     * @return instancia de {@link ProductosErrorDecoder}.
     */
    @Bean
    public feign.codec.ErrorDecoder errorDecoder() {
        return new com.proyecto.servicios.client.decoder.ProductosErrorDecoder();
    }

    /**
     * Define el nivel de registro (logging) para el cliente Feign.
     *
     * @return nivel {@link Logger.Level#BASIC} para registrar método, URL, status y tiempo de respuesta sin exponer payloads sensibles.
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

