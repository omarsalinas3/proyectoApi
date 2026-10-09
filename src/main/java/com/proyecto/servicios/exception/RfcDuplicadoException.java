package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se intenta registrar un RFC que ya existe en el sistema.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class RfcDuplicadoException extends RuntimeException {

    public RfcDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
