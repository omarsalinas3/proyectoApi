package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando GestoPago responde un código distinto de "01" (ej. 02 saldo insuficiente del
 * distribuidor, 06 transacción duplicada); el monto retenido al cliente ya fue liberado (nodo «Sen Tx Cifrado»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
public class TransaccionRechazadaException extends RuntimeException {

    private final String upc;
    private final String codigoProveedor;

    public TransaccionRechazadaException(String upc, String codigoProveedor, String mensaje) {
        super(mensaje);
        this.upc = upc;
        this.codigoProveedor = codigoProveedor;
    }
}
