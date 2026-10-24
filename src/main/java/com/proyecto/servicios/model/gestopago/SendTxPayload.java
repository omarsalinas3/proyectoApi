package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload JSON de {@code sendTx.do} / {@code confirmTx.do} antes de cifrarse, con los nombres y longitudes de la
 * documentación de GestoPago:
 *
 * <table>
 *   <tr><th>Campo</th><th>Tipo</th><th>Longitud</th></tr>
 *   <tr><td>idProducto</td><td>integer</td><td>-</td></tr>
 *   <tr><td>idServicio</td><td>integer</td><td>-</td></tr>
 *   <tr><td>telefono</td><td>string</td><td>10 - 20</td></tr>
 *   <tr><td>horaLocal</td><td>string (yyyyMMddHHmmss)</td><td>1 - 15</td></tr>
 *   <tr><td>referencia</td><td>string (solo pago de servicios)</td><td>1 - 100</td></tr>
 *   <tr><td>montoPago</td><td>double (solo pago de servicios)</td><td>-</td></tr>
 *   <tr><td>upc</td><td>string</td><td>3 - 15</td></tr>
 *   <tr><td>unidad</td><td>string</td><td>3 - 25</td></tr>
 * </table>
 *
 * <p>Los campos nulos no se envían (recargas y PINes no llevan {@code referencia} ni {@code montoPago}).</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"idProducto", "idServicio", "telefono", "horaLocal", "referencia", "montoPago", "upc", "unidad"})
public class SendTxPayload {

    public static final int TELEFONO_MIN = 10;
    public static final int TELEFONO_MAX = 20;
    public static final int HORA_LOCAL_MAX = 15;
    public static final int REFERENCIA_MAX = 100;
    public static final int UPC_MIN = 3;
    public static final int UPC_MAX = 15;
    public static final int UNIDAD_MIN = 3;
    public static final int UNIDAD_MAX = 25;

    private Long idProducto;
    private Integer idServicio;
    private String telefono;
    private String horaLocal;
    private String referencia;
    private BigDecimal montoPago;
    private String upc;
    private String unidad;
}
