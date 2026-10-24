package com.proyecto.servicios.model.transaccion.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado de una transacción enviada a GestoPago.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resultado de la transacción")
public class TransaccionResponse {

    @Schema(description = "Identificador único de la operación (UPC)", example = "TX4K9Z2M7Q1A8B3")
    private String upc;

    @Schema(description = "Estatus de la transacción", example = "EXITOSA")
    private EstatusTransaccionEnum estatus;

    @Schema(description = "Número de cuenta con la que se pagó", example = "4152262623176650")
    private String numCuenta;

    @Schema(description = "Monto cargado a la cuenta", example = "10.00")
    private BigDecimal monto;

    @Schema(description = "Saldo disponible de la cuenta después de la operación", example = "490.00")
    private BigDecimal saldoDisponible;

    @Schema(description = "Número de transacción de GestoPago (ID_TX)", example = "291437188")
    private Long idTransaccionProveedor;

    @Schema(description = "Número de autorización de GestoPago", example = "100024988")
    private String numeroAutorizacion;

    @Schema(description = "Código de respuesta de GestoPago (01 = éxito)", example = "01")
    private String codigoProveedor;

    @Schema(description = "Mensaje de GestoPago", example = "Operacion realizada con exito")
    private String mensajeProveedor;

    @Schema(description = "Fecha de la operación según GestoPago", example = "25/Feb/2022 18:15:25")
    private String fechaProveedor;

    @Schema(description = "Referencia aplicada (pago de servicios)", example = "25720381")
    private String referencia;

    @Schema(description = "PIN entregado (tarjetas de regalo / entretenimiento)")
    private String pin;
}
