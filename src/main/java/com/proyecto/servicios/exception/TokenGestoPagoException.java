package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando no fue posible obtener o renovar el token de autorización de GestoPago (nodo «Obtener Token»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class TokenGestoPagoException extends RuntimeException {

    public TokenGestoPagoException(String mensaje) {
        super(mensaje);
    }
}
