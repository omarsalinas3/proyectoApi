package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando no existe una transacción con el UPC indicado.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class TransaccionNoEncontradaException extends RuntimeException {

    public TransaccionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
