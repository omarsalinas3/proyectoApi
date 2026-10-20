package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Request DTO para la actualización de información de Clientes Personas Físicas.
 *
 * <p><strong>Restricción de Negocio:</strong>
 * No se permite modificar CURP, RFC, número de cuenta, saldos, estatus ni fechas de registro: si la petición
 * los incluye se rechaza con 400 (ver {@link RequestEstricto}).
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Petición para modificar datos permitidos del cliente (sin CURP, RFC ni cuenta)")
@EqualsAndHashCode(callSuper = false)
public class ClienteUpdateRequest extends RequestEstricto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Identificador del cliente a actualizar", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = PatronesValidacion.MENSAJE_ID)
    private Long id;

    @Schema(description = "Primer nombre del cliente", example = "Carlos")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Schema(description = "Segundo nombre (opcional)", example = "Eduardo")
    @Size(max = 50, message = "El segundo nombre no debe exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @Schema(description = "Apellido paterno", example = "Hernández")
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno", example = "López")
    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @Schema(description = "Sexo", example = "M")
    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "(?i)^(M|F|H|MASCULINO|FEMENINO|HOMBRE|MUJER|OTRO)$",
            message = "El sexo no es válido. Valores permitidos: M, F, MASCULINO, FEMENINO, OTRO")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(min = 4, max = 50, message = "La nacionalidad debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$",
            message = "La nacionalidad solo debe contener letras y espacios simples (sin números ni símbolos), por ejemplo: Mexicana")
    private String nacionalidad;

    @Schema(description = "Estado civil", example = "CASADO")
    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "(?i)^(SOLTERO|SOLTERA|CASADO|CASADA|UNION[ _]?LIBRE|UNIÓN[ _]?LIBRE|CONCUBINATO|DIVORCIADO|DIVORCIADA|VIUDO|VIUDA)$",
            message = "El estado civil no es válido. Valores permitidos: SOLTERO, CASADO, UNION_LIBRE, DIVORCIADO, VIUDO")
    private String estadoCivil;

    @Schema(description = "Correo electrónico", example = "carlos.nuevo@example.com")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo electrónico debe tener el formato usuario@dominio.com (con dominio y extensión válidos)")
    @Size(max = 100, message = "El correo no debe exceder 100 caracteres")
    private String correo;

    @Schema(description = "Teléfono móvil a 10 dígitos", example = "5599887766")
    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo", example = "5511223344")
    @Pattern(regexp = "^(\\d{10})?$", message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    private String telefonoAlternativo;

    @Schema(description = "Ocupación o puesto actual", example = "Líder Técnico")
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(min = 2, max = 100, message = "La ocupación debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .,-]*$",
            message = "La ocupación solo debe contener letras, espacios, puntos, comas y guiones (sin números ni otros símbolos)")
    private String ocupacion;

    @Schema(description = "Empresa actual", example = "Global Bank México")
    @NotBlank(message = "La empresa es obligatoria")
    @Size(min = 2, max = 100, message = "La empresa debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9 .,&'-]*$",
            message = "La empresa solo debe contener letras, números, espacios y los símbolos . , & ' -")
    private String empresa;

    @Schema(description = "Ingreso mensual comprobable", example = "45000.00")
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @Schema(description = "Domicilio actualizado del cliente")
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;
}
