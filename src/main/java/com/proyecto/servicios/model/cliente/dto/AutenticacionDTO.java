package com.proyecto.servicios.model.cliente.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO para la autenticación inicial del cliente (contraseña y biométrico Face ID).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Datos de autenticación inicial del cliente")
public class AutenticacionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Contraseña de acceso para el usuario", example = "Password123*")
    @JsonAlias({"contrasena", "clave"})
    private String password;

    @Schema(description = "Identificador o dato biométrico Face ID numérico", example = "778899001122")
    @JsonAlias({"faceIdBiometrico", "biometrico", "face_id"})
    private Long faceId;
}
