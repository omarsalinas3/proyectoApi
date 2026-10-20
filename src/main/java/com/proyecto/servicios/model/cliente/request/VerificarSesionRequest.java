package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Parámetros opcionales para verificar la sesión del usuario autenticado.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Parámetros opcionales de verificación de sesión")
public class VerificarSesionRequest extends RequestEstricto {

    @Schema(description = "Límite de inactividad en segundos a evaluar en esta consulta (opcional)", example = "300")
    @Positive(message = "El tiempo de inactividad debe ser mayor a 0 segundos")
    @Max(value = 86400, message = "El tiempo de inactividad no debe exceder 86400 segundos (24 horas)")
    private Integer timeoutDinamico;
}
