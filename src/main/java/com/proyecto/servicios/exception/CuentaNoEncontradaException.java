package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando no se encuentra una cuenta bancaria por su identificador o número de cuenta.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CuentaNoEncontradaException extends RuntimeException {

    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
