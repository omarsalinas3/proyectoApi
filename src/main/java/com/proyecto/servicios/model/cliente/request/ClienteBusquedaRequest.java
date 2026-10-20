package com.proyecto.servicios.model.cliente.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Criterios de búsqueda de clientes. Todos los filtros son opcionales y se combinan con AND;
 * sin filtros se devuelven todos los clientes (paginados).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Filtros combinables para la búsqueda de clientes (todos opcionales)")
public class ClienteBusquedaRequest extends RequestEstricto {

    public static final int TAMANIO_MAXIMO = 100;

    @Schema(description = "Identificador del cliente", example = "1")
    @Positive(message = PatronesValidacion.MENSAJE_ID)
    private Long id;

    @Schema(description = "CURP exacta", example = "HELC950815HDFRPR03")
    @Pattern(regexp = PatronesValidacion.CURP, message = PatronesValidacion.MENSAJE_CURP)
    private String curp;

    @Schema(description = "RFC exacto", example = "HELC950815ABC")
    @Pattern(regexp = PatronesValidacion.RFC, message = PatronesValidacion.MENSAJE_RFC)
    private String rfc;

    @Schema(description = "Correo electrónico exacto", example = "carlos.hernandez@example.com")
    @Email(message = "El formato del correo electrónico no es válido (ej. usuario@dominio.com)")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correo;

    @Schema(description = "Número de cuenta de 16 dígitos", example = "4152000000000001")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "Texto contenido en el nombre o apellidos (sin distinguir mayúsculas)", example = "hernandez")
    @Size(min = 2, max = 50, message = "El nombre a buscar debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ ]+$", message = "El nombre a buscar solo debe contener letras y espacios")
    private String nombre;

    @Schema(description = "true = solo activos, false = solo dados de baja; omitir para ambos", example = "true")
    private Boolean activo;

    @Schema(description = "Fecha de registro inicial, inclusive (YYYY-MM-DD)", example = "2026-01-01")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @PastOrPresent(message = "La fecha de registro inicial no puede ser una fecha futura")
    private LocalDate fechaRegistroDesde;

    @Schema(description = "Fecha de registro final, inclusive (YYYY-MM-DD)", example = "2026-12-31")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaRegistroHasta;

    @Schema(description = "Número de página (inicia en 0)", example = "0", defaultValue = "0")
    @Min(value = 0, message = "La página debe ser mayor o igual a 0")
    @Builder.Default
    private Integer pagina = 0;

    @Schema(description = "Elementos por página (1 a 100)", example = "20", defaultValue = "20")
    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = TAMANIO_MAXIMO, message = "El tamaño de página no debe exceder " + TAMANIO_MAXIMO)
    @Builder.Default
    private Integer tamanio = 20;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "La fecha de registro inicial no puede ser posterior a la fecha final")
    public boolean isRangoFechaRegistroValido() {
        return fechaRegistroDesde == null || fechaRegistroHasta == null || !fechaRegistroDesde.isAfter(fechaRegistroHasta);
    }
}
