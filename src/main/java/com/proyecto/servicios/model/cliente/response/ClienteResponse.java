package com.proyecto.servicios.model.cliente.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Response DTO estandarizado con los datos de un Cliente Persona Física y sus productos asociados.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta detallada del cliente persona física")
public class ClienteResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador único interno del cliente", example = "1")
    private Long id;

    @Schema(description = "Nombre(s)", example = "Carlos")
    private String nombre;

    @Schema(description = "Segundo nombre", example = "Eduardo")
    private String segundoNombre;

    @Schema(description = "Apellido paterno", example = "Hernández")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno", example = "López")
    private String apellidoMaterno;

    @Schema(description = "Nombre completo formateado", example = "Carlos Eduardo Hernández López")
    private String nombreCompleto;

    @Schema(description = "Fecha de nacimiento (YYYY-MM-DD)", example = "1995-08-15")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @Schema(description = "Clave Única de Registro de Población", example = "HELC950815HDFRPR03")
    private String curp;

    @Schema(description = "Registro Federal de Contribuyentes", example = "HELC950815ABC")
    private String rfc;

    @Schema(description = "Sexo", example = "M")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    private String nacionalidad;

    @Schema(description = "Estado civil", example = "SOLTERO")
    private String estadoCivil;

    @Schema(description = "Correo electrónico", example = "carlos.hernandez@example.com")
    private String correo;

    @Schema(description = "Teléfono móvil", example = "5512345678")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo", example = "5587654321")
    private String telefonoAlternativo;

    @Schema(description = "Ocupación laboral", example = "Ingeniero de Software")
    private String ocupacion;

    @Schema(description = "Empresa donde labora", example = "Tech Solutions S.A. de C.V.")
    private String empresa;

    @Schema(description = "Ingreso mensual comprobable", example = "35000.00")
    private BigDecimal ingresoMensual;

    @Schema(description = "Estatus activo del cliente (Baja lógica)", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de registro en el sistema (YYYY-MM-DD)", example = "2026-10-02")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaRegistro;

    @Schema(description = "Domicilio particular del cliente")
    private DomicilioDTO domicilio;

    @Schema(description = "Cuenta bancaria asociada al cliente")
    private CuentaDTO cuenta;

    @Schema(description = "Detalle de saldos de la cuenta bancaria")
    private SaldoDTO saldo;
}
