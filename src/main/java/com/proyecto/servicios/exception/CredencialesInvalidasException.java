package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando las credenciales de acceso (usuario, contraseña o biometría) no coinciden.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
