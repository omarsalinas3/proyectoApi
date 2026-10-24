package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el cliente no tiene activado el segundo factor de autenticación (2FA).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class DosFactoresNoConfiguradoException extends RuntimeException {

    public DosFactoresNoConfiguradoException(String mensaje) {
        super(mensaje);
    }
}
