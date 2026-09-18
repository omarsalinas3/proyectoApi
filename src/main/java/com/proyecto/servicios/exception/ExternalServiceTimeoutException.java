package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando la comunicación con el servicio externo excede los tiempos límite
 * configurados (Connect Timeout o Read Timeout).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ExternalServiceTimeoutException extends RuntimeException {

    /**
     * Construye una nueva excepción de timeout de servicio externo.
     *
     * @param message descripción del evento de timeout.
     */
    public ExternalServiceTimeoutException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción de timeout con causa raíz.
     *
     * @param message descripción del evento de timeout.
     * @param cause   causa raíz (ej. SocketTimeoutException o RetryableException).
     */
    public ExternalServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
