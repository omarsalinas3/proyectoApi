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
import java.time.OffsetDateTime;

/**
 * Data Transfer Object para la Cuenta Bancaria.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos estructurados de la cuenta bancaria")
public class CuentaDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Número de cuenta bancaria único de 16 dígitos", example = "1234567890123456")
    private String numeroCuenta;

    @Schema(description = "Saldo inicial asignado por el sistema", example = "0.00")
    private BigDecimal saldoInicial;

    @Schema(description = "Saldo disponible para operaciones", example = "0.00")
    private BigDecimal saldoDisponible;

    @Schema(description = "Estatus actual de la cuenta (ACTIVA, BLOQUEADA, CANCELADA)", example = "ACTIVA")
    private String estatus;

    @Schema(description = "Fecha y hora de apertura de la cuenta")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime fechaApertura;
}
