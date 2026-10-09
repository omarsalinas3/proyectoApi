package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * DTO para la información financiera del cliente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Información financiera del cliente")
public class InformacionFinancieraDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Ocupación o profesión del cliente", example = "Ingeniero de Software")
    private String ocupacion;

    @Schema(description = "Ingreso mensual neto aproximado", example = "45000.00")
    private BigDecimal ingresoMensual;
}
