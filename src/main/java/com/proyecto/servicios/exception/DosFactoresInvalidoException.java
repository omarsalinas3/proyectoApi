package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el código del segundo factor (2FA) es incorrecto, expiró o ya fue utilizado (nodo «2FA»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class DosFactoresInvalidoException extends RuntimeException {

    public DosFactoresInvalidoException(String mensaje) {
        super(mensaje);
    }
}
