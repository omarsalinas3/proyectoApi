package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 100, message = "La calle no debe exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9 .,#'-]*$", message = "La calle solo debe contener letras, números, espacios y los símbolos . , # ' -")
    private String calle;

    @Schema(description = "Número exterior", example = "string")
    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no debe exceder 10 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9 /-]*$", message = "El número exterior solo debe contener letras, números, espacios, / y -")
    private String numeroExterior;

    @Schema(description = "Número interior (opcional)", example = "string")
    @Size(max = 10, message = "El número interior no debe exceder 10 caracteres")
    @Pattern(regexp = "^([A-Za-z0-9][A-Za-z0-9 /-]*)?$", message = "El número interior solo debe contener letras, números, espacios, / y -")
    private String numeroInterior;

    @Schema(description = "Colonia o fraccionamiento", example = "string")
    @NotBlank(message = "La colonia es obligatoria")
    @Size(max = 100, message = "La colonia no debe exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9 .,#'-]*$", message = "La colonia solo debe contener letras, números, espacios y los símbolos . , # ' -")
    private String colonia;

    @Schema(description = "Municipio o Alcaldía", example = "string")
    @NotBlank(message = "El municipio es obligatorio")
    @Size(max = 100, message = "El municipio no debe exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .-]*$", message = "El municipio solo debe contener letras, espacios, puntos y guiones (sin números)")
    private String municipio;

    @Schema(description = "Estado de la República", example = "string")
    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 100, message = "El estado no debe exceder 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .-]*$", message = "El estado solo debe contener letras, espacios, puntos y guiones (sin números)")
    private String estado;

    @Schema(description = "Código Postal mexicano de 5 dígitos", example = "49058")
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos numéricos")
    private String codigoPostal;

    @Schema(description = "País de residencia", example = "string")
    @Builder.Default
    @NotBlank(message = "El país es obligatorio")
    @Size(max = 60, message = "El país no debe exceder 60 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .-]*$", message = "El país solo debe contener letras, espacios, puntos y guiones (sin números)")
    private String pais = "string";
}
