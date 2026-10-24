package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el saldo disponible de la cuenta no cubre el monto de la operación (nodo «Saldo»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
