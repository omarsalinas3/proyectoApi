package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Data Transfer Object para los datos personales del cliente en operaciones Layaway.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos personales del cliente")
public class DatosPersonalesDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Primer nombre cifrado o en texto plano", example = "YQzGkAQzllOJMyAdQFcmTwEhAKPdDseRYLmcUHBRqJiLqrSnbW")
    private String nombre;

    @Schema(description = "Segundo nombre cifrado o en texto plano", example = "LgvzPETOYKzBakvBqovPOSWvEBzSGAvEdrOXsHSIJeuKAJgeCE")
    private String segundoNombre;

    @Schema(description = "Apellido paterno cifrado o en texto plano", example = "oytmQLZyyyBJQrfXUHSxdldJVm")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno cifrado o en texto plano", example = "KMmNeXaLmHsEuvmSSNeg NwaePCsyvagEjpTXDIuCziw ykLhi")
    private String apellidoMaterno;

    @Schema(description = "Fecha de nacimiento (YYYY-MM-DD)", example = "2026-09-30")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @Schema(description = "Clave Única de Registro de Población", example = "DXDC342865HFQQVFP4")
    private String curp;

    @Schema(description = "Registro Federal de Contribuyentes", example = "NZCX400561GY1")
    private String rfc;

    @Schema(description = "Sexo", example = "string")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "string")
    private String nacionalidad;

    @Schema(description = "Estado civil", example = "string")
    private String estadoCivil;

    @Schema(description = "Identificador o dato biométrico numérico", example = "0")
    private Long datosBiometricos;
}
