package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando ocurre un error de autenticación o autorización (HTTP 401 / 403)
 * al consumir el servicio externo.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ExternalServiceAuthException extends RuntimeException {

    private final int statusCode;

    /**
     * Construye una nueva excepción de autenticación de servicio externo.
     *
     * @param message    descripción del error sin exponer credenciales.
     * @param statusCode código HTTP retornado (401 o 403).
     */
    public ExternalServiceAuthException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Obtiene el código de estado HTTP.
     *
     * @return código HTTP de autenticación.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
