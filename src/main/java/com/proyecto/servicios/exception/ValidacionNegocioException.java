package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando ocurre una infracción a las reglas de negocio (ej. modificación de campos inmutables, minoría de edad, etc.).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ValidacionNegocioException extends RuntimeException {

    public ValidacionNegocioException(String mensaje) {
        super(mensaje);
    }
}
