package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el cliente o su cuenta no están activos y por lo tanto no pueden operar transacciones (nodo «Cliente Activo»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ClienteInactivoException extends RuntimeException {

    public ClienteInactivoException(String mensaje) {
        super(mensaje);
    }
}
