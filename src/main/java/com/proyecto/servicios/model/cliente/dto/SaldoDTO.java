package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object para el balance detallado de saldos.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Detalle de saldos de la cuenta bancaria")
public class SaldoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Número de cuenta asociada", example = "1234567890123456")
    private String numeroCuenta;

    @Schema(description = "Saldo total actual en la cuenta", example = "0.00")
    private BigDecimal saldoActual;

    @Schema(description = "Saldo retenido por transacciones pendientes", example = "0.00")
    private BigDecimal saldoRetenido;

    @Schema(description = "Saldo disponible para retiro o transferencias", example = "0.00")
    private BigDecimal saldoDisponible;

    @Schema(description = "Código de moneda", example = "MXN")
    private String moneda;

    @Schema(description = "Fecha de última actualización del saldo (YYYY-MM-DD)", example = "2026-10-02")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaActualizacion;
}
