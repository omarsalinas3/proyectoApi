package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando falla el cifrado o descifrado AES del payload de GestoPago (nodo «Cifrar Petición»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CifradoGestoPagoException extends RuntimeException {

    public CifradoGestoPagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
