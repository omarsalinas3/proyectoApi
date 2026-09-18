package com.proyecto.servicios.client.decoder;

import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.ExternalServiceTimeoutException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;

/**
 * Decodificador de errores personalizado para el cliente Feign de productos.
 *
 * <p>Traduce los códigos de estado HTTP de respuesta del servicio externo a excepciones
 * de dominio fuertemente tipadas sin exponer información sensible o tokens.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
public class ProductosErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reason = response.reason() != null ? response.reason() : "Sin descripción";

        log.error("Error en respuesta de servicio externo [{}] - Status HTTP: {}, Razón: {}", methodKey, status, reason);

        switch (status) {
            case 401:
                return new ExternalServiceAuthException("Error de autenticación: Token no válido o expirado para el servicio externo", status);
            case 403:
                return new ExternalServiceAuthException("Acceso prohibido: No cuenta con permisos suficientes en el servicio externo", status);
            case 408:
            case 504:
                return new ExternalServiceTimeoutException("Tiempo de espera agotado al consultar el servicio externo");
            case 404:
                return new ExternalServiceException("El recurso solicitado no fue encontrado en el servicio externo", status);
            case 500:
            case 502:
            case 503:
                return new ExternalServiceException("El servicio externo no está disponible temporalmente o presentó un error interno", status);
            default:
                if (status >= 400 && status < 500) {
                    return new ExternalServiceException("Petición incorrecta enviada al servicio externo: " + reason, status);
                }
                return defaultErrorDecoder.decode(methodKey, response);
        }
    }
}
