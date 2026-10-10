package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se intenta registrar o asociar una CURP que ya existe en el sistema.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CurpDuplicadaException extends RuntimeException {

    public CurpDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
