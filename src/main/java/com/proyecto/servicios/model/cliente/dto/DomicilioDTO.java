package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object para la información de domicilio del cliente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos estructurados de domicilio del cliente")
public class DomicilioDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Calle de residencia", example = "string")
    private String calle;

    @Schema(description = "Número exterior", example = "string")
    private String numeroExterior;

    @Schema(description = "Número interior (opcional)", example = "string")
    private String numeroInterior;

    @Schema(description = "Colonia o fraccionamiento", example = "string")
    private String colonia;

    @Schema(description = "Municipio o Alcaldía", example = "string")
    private String municipio;

    @Schema(description = "Estado de la República", example = "string")
    private String estado;

    @Schema(description = "Código Postal mexicano de 5 dígitos", example = "49058")
    private String codigoPostal;

    @Schema(description = "País de residencia", example = "string")
    @Builder.Default
    private String pais = "string";
}
