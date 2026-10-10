package com.proyecto.servicios.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modelo estándar para la respuesta de errores de la API.
 *
 * <p><strong>Reglas de diseño:</strong>
 * <ul>
 *   <li>Exclusión de nulos: {@link JsonInclude.Include#NON_NULL} omite campos como 'detalle' si no están presentes.</li>
 *   <li>Tipado estricto: {@link Integer} para códigos de estado y {@link LocalDateTime} formateado para fechas.</li>
 * </ul>
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
public class ErrorResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Código de estado o código de error de negocio.
     */
    @JsonProperty("codigo")
    private Integer codigo;

    /**
     * Descripción general del error ocurrido.
     */
    @JsonProperty("mensaje")
    private String mensaje;

    /**
     * Detalle específico del error (opcional y seguro, sin exponer datos sensibles).
     */
    @JsonProperty("detalle")
    private String detalle;

    /**
     * Estampa de tiempo exacta en que se originó el error.
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
