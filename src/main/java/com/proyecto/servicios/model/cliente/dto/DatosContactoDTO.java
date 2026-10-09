package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object para los datos de contacto del cliente en operaciones Layaway.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos de contacto del cliente")
public class DatosContactoDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Correo electrónico", example = "string")
    private String correo;

    @Schema(description = "Teléfono móvil a 10 dígitos", example = "7944140881")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo u oficina", example = "")
    private String telefonoAlt;
}
