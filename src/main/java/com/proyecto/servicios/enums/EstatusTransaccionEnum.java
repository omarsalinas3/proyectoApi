package com.proyecto.servicios.enums;

/**
 * Ciclo de vida de una transacción enviada a GestoPago.
 *
 * <ul>
 *   <li>{@code PENDIENTE}: validada, con el monto retenido, en envío a GestoPago.</li>
 *   <li>{@code EXITOSA}: GestoPago respondió CODIGO 01 (o confirmTx la reportó aplicada); el monto se cargó.</li>
 *   <li>{@code RECHAZADA}: GestoPago respondió un código de error; el monto retenido se liberó.</li>
 *   <li>{@code PENDIENTE_CONFIRMACION}: timeout (CODIGO 82 o sin respuesta); el monto sigue retenido hasta
 *   resolverla con confirmTx (no antes de 62 s ni después de 24 h del envío).</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public enum EstatusTransaccionEnum {
    PENDIENTE,
    EXITOSA,
    RECHAZADA,
    PENDIENTE_CONFIRMACION
}
