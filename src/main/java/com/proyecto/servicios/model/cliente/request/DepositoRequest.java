package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Abono de saldo a una cuenta (operación de ejecutivo).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Abono de saldo a una cuenta")
public class DepositoRequest extends RequestEstricto {

    @Schema(description = "Número de cuenta de 16 dígitos", example = "4152262623176650", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "Monto a abonar", example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @DecimalMax(value = "1000000.00", message = "El monto no debe exceder 1,000,000.00 por depósito")
    @Digits(integer = 7, fraction = 2, message = "El monto debe tener como máximo 2 decimales")
    private BigDecimal monto;
}
