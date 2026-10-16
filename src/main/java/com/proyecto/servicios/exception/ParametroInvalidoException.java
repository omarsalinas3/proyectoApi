package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando un parámetro de ruta o de consulta no cumple el formato esperado (ej. ID no positivo, rango de fechas invertido).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ParametroInvalidoException extends RuntimeException {

    public ParametroInvalidoException(String mensaje) {
        super(mensaje);
    }
}
