package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Cuenta de la que se consulta el saldo.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Cuenta de la que se consulta el saldo")
public class CuentaSaldoRequest extends RequestEstricto {

    @Schema(description = "Número de cuenta de 16 dígitos", example = "4152000000000001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;
}
