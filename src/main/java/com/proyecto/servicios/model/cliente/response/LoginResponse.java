package com.proyecto.servicios.model.cliente.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Response DTO para la autenticación y estado de sesión del usuario.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de autenticación y estado de sesión")
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Código de respuesta estandarizado", example = "EXITO")
    @JsonProperty("codigoRespuesta")
    private CodigoRespuestaEnum codigoRespuesta;

    @Schema(description = "Código numérico (0 = Éxito)", example = "0")
    @JsonProperty("codigo")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Inicio de sesión exitoso")
    @JsonProperty("mensaje")
    private String mensaje;

    @Schema(description = "Identificador del usuario", example = "1")
    private Long usuarioId;

    @Schema(description = "Nombre de usuario", example = "carlos.h95")
    private String username;

    @Schema(description = "Estado de sesión booleano (true = Activa, false = Inactiva/Expirada)", example = "true")
    private Boolean sesionActiva;

    @Schema(description = "Tiempo límite de inactividad configurado en segundos", example = "300")
    private Integer tiempoInactividadSegundos;

    @Schema(description = "Segundos restantes de inactividad antes de que la sesión expire", example = "298")
    private Long segundosRestantesInactividad;

    @Schema(description = "Estampa de tiempo de la última actividad registrada")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX")
    private OffsetDateTime ultimaActividad;

    @Schema(description = "Token de sesión generado", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
}
