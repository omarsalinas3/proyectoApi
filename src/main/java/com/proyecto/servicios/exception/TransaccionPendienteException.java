package com.proyecto.servicios.exception;

import lombok.Getter;

/**
 * Excepción lanzada cuando GestoPago no confirma el resultado dentro del tiempo límite (CODIGO 82 o sin
 * respuesta). La transacción queda PENDIENTE_CONFIRMACION con el monto retenido hasta consultar confirmTx.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
public class TransaccionPendienteException extends RuntimeException {

    private final String upc;

    public TransaccionPendienteException(String upc, String mensaje) {
        super(mensaje);
        this.upc = upc;
    }
}
