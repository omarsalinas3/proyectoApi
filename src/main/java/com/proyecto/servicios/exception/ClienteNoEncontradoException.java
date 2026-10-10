package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando no se encuentra un cliente por su ID, CURP, RFC o número de cuenta.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
