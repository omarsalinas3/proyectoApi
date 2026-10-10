package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el nombre de usuario de acceso ya está registrado.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class UsuarioDuplicadoException extends RuntimeException {

    public UsuarioDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
