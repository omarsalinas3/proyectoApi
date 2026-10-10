package com.proyecto.servicios.model.cliente.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Request DTO para el inicio de sesión con soporte para contraseña y Face ID biométrico.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Petición de autenticación de usuario")
public class LoginRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Bandera de inicio de sesión: 1 = Contraseña, 2 = Biométrico Face ID", example = "0")
    private Integer bandera;

    @Schema(description = "Nombre de usuario o correo", example = "omar.salinas@test.com")
    @com.fasterxml.jackson.annotation.JsonAlias({"usuario", "correo", "user", "email"})
    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String username;

    @Schema(description = "Contraseña del usuario (requerido si Bandera = 1)", example = "Password123*")
    @com.fasterxml.jackson.annotation.JsonAlias({"contrasena", "clave"})
    private String password;

    @Schema(description = "Identificador o dato biométrico Face ID numérico (requerido si Bandera = 2)", example = "987654321098")
    @com.fasterxml.jackson.annotation.JsonAlias({"faceId", "biometrico", "face_id"})
    private Long faceIdBiometrico;
}
