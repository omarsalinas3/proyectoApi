package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando se intenta registrar un correo electrónico que ya existe en el sistema.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CorreoDuplicadoException extends RuntimeException {

    public CorreoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
