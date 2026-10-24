package com.proyecto.servicios.model.gestopago;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Respuesta XML de {@code sendTx.do} / {@code confirmTx.do} ya interpretada.
 *
 * <pre>
 * &lt;RESPONSE&gt;
 *   &lt;ID_TX&gt;291437188&lt;/ID_TX&gt;&lt;NUM_AUTORIZACION&gt;100024988&lt;/NUM_AUTORIZACION&gt;
 *   &lt;SALDO&gt;157731.83&lt;/SALDO&gt;&lt;COMISION&gt;0.7&lt;/COMISION&gt;&lt;FECHA&gt;25/Feb/2022 18:15:25&lt;/FECHA&gt;&lt;MONTO&gt;10.0&lt;/MONTO&gt;
 *   &lt;MENSAJE&gt;&lt;CODIGO&gt;01&lt;/CODIGO&gt;&lt;TEXTO&gt;Operacion realizada con exito&lt;/TEXTO&gt;&lt;REFERENCIA/&gt;&lt;/MENSAJE&gt;
 * &lt;/RESPONSE&gt;
 * </pre>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestoPagoTxResultado {

    /** Operación aplicada. Cualquier otro código es error. */
    public static final String CODIGO_EXITO = "01";
    /** confirmTx: "La transacción fue aplicada exitosamente" (en sendTx: transacción duplicada). */
    public static final String CODIGO_APLICADA_ANTERIORMENTE = "06";
    /** confirmTx: la transacción no fue aplicada. */
    public static final String CODIGO_NO_APLICADA = "70";
    /** Tiempo de espera alcanzado: el resultado se debe consultar con confirmTx. */
    public static final String CODIGO_TIEMPO_ESPERA = "82";

    private String codigo;
    private String texto;
    private Long idTx;
    private String numAutorizacion;
    private BigDecimal saldo;
    private BigDecimal comision;
    private BigDecimal monto;
    private String fecha;
    private String referencia;
    private String pin;
    private String legend;

    public boolean esExitosa() {
        return CODIGO_EXITO.equals(codigo);
    }

    public boolean esTiempoDeEsperaAgotado() {
        return CODIGO_TIEMPO_ESPERA.equals(codigo);
    }

    /** Para confirmTx: aplicada si trae número de autorización positivo y código 01 o 06. */
    public boolean confirmaAplicada() {
        return (CODIGO_EXITO.equals(codigo) || CODIGO_APLICADA_ANTERIORMENTE.equals(codigo)) && autorizacionValida();
    }

    private boolean autorizacionValida() {
        try {
            return numAutorizacion != null && Long.parseLong(numAutorizacion.trim()) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
