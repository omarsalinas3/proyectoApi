package com.proyecto.servicios.model.common;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Estructura uniforme de todas las respuestas de la API, exitosas o de error.
 *
 * <pre>
 * Éxito: { "codigo": 0, "codigoRespuesta": "EXITO", "mensaje": "...", "datos": {...}, "timestamp": "..." }
 * Error: { "codigo": 41, "codigoRespuesta": "ERROR_PARAMETRO_INVALIDO", "mensaje": "...", "detalle": "...",
 *          "errores": { "correo": "El formato del correo electrónico no es válido" }, "timestamp": "..." }
 * </pre>
 *
 * @param <T> tipo del contenido de {@code datos}.
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"codigo", "codigoRespuesta", "mensaje", "detalle", "errores", "datos", "timestamp"})
@Schema(description = "Respuesta uniforme de la API (éxito o error)")
public class RespuestaApi<T> {

    @Schema(description = "Código numérico del resultado (0 = éxito)", example = "0")
    private Integer codigo;

    @Schema(description = "Código de respuesta estandarizado", example = "EXITO")
    private CodigoRespuestaEnum codigoRespuesta;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Cliente consultado exitosamente")
    private String mensaje;

    @Schema(description = "Explicación detallada del error (solo en errores)",
            example = "La petición contiene 2 campos con errores de validación")
    private String detalle;

    @Schema(description = "Errores de validación por campo: nombre del campo → motivo (solo en errores)",
            example = "{\"correo\": \"El formato del correo electrónico no es válido\"}")
    private Map<String, String> errores;

    @Schema(description = "Contenido de la respuesta (solo en éxito)")
    private T datos;

    @Schema(description = "Fecha y hora de la respuesta", example = "2026-10-20 10:15:30")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Respuesta exitosa con contenido.
     */
    public static <T> RespuestaApi<T> exito(String mensaje, T datos) {
        return RespuestaApi.<T>builder()
                .codigo(CodigoRespuestaEnum.EXITO.getCodigo())
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .mensaje(mensaje)
                .datos(datos)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Respuesta de error con detalle y, opcionalmente, errores por campo.
     */
    public static <T> RespuestaApi<T> error(CodigoRespuestaEnum codigo, String detalle, Map<String, String> errores) {
        return RespuestaApi.<T>builder()
                .codigo(codigo.getCodigo())
                .codigoRespuesta(codigo)
                .mensaje(codigo.getDescripcion())
                .detalle(detalle)
                .errores(errores == null || errores.isEmpty() ? null : errores)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Respuesta de error sin errores por campo.
     */
    public static <T> RespuestaApi<T> error(CodigoRespuestaEnum codigo, String detalle) {
        return error(codigo, detalle, null);
    }
}
