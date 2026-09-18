package com.proyecto.servicios.exception;

/**
 * Excepción base para fallos o respuestas no exitosas en la integración con servicios externos.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ExternalServiceException extends RuntimeException {

    private final int statusCode;

    /**
     * Construye una nueva excepción de servicio externo con mensaje y código de estado.
     *
     * @param message    descripción del error.
     * @param statusCode código de estado HTTP o código de error retornado.
     */
    public ExternalServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Construye una nueva excepción de servicio externo con mensaje, causa y código de estado.
     *
     * @param message    descripción del error.
     * @param cause      causa original del error.
     * @param statusCode código de estado HTTP retornado.
     */
    public ExternalServiceException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Obtiene el código de estado HTTP asociado al error.
     *
     * @return código de estado entero.
     */
    public int getStatusCode() {
        return statusCode;
    }
}
