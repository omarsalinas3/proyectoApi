package com.proyecto.servicios.model.cliente.response;

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

    @Schema(description = "Fecha de la última actividad registrada (YYYY-MM-DD)", example = "2026-10-02")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate ultimaActividad;

    @Schema(description = "Token JWT que debe enviarse en el encabezado 'Authorization: Bearer <token>'", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Tipo de token", example = "Bearer")
    private String tipoToken;

    @Schema(description = "Vigencia del token en segundos", example = "1800")
    private Long expiraEnSegundos;

    @Schema(description = "Rol del usuario autenticado (CLIENTE o EJECUTIVO)", example = "CLIENTE")
    private String rol;
}
