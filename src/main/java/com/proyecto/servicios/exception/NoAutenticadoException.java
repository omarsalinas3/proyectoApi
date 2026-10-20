package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando una operación protegida se invoca sin un token de acceso válido.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class NoAutenticadoException extends RuntimeException {

    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
