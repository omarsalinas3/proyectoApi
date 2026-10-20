package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(callSuper = false)
public class LoginRequest extends RequestEstricto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Bandera de inicio de sesión: 1 = Contraseña (por defecto), 2 = Biométrico Face ID", example = "1")
    @Min(value = 1, message = "La bandera debe ser 1 (Contraseña) o 2 (Biométrico Face ID)")
    @Max(value = 2, message = "La bandera debe ser 1 (Contraseña) o 2 (Biométrico Face ID)")
    private Integer bandera;

    @Schema(description = "Nombre de usuario o correo", example = "omar.salinas@test.com")
    @com.fasterxml.jackson.annotation.JsonAlias({"usuario", "correo", "user", "email"})
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 100, message = "El nombre de usuario no debe exceder 100 caracteres")
    private String username;

    @Schema(description = "Contraseña del usuario (requerido si Bandera = 1)", example = "Password123*")
    @com.fasterxml.jackson.annotation.JsonAlias({"contrasena", "clave"})
    @Size(max = 72, message = "La contraseña no debe exceder 72 caracteres")
    private String password;

    @Schema(description = "Identificador o dato biométrico Face ID numérico (requerido si Bandera = 2)", example = "987654321098")
    @com.fasterxml.jackson.annotation.JsonAlias({"faceId", "biometrico", "face_id"})
    @Positive(message = "El Face ID biométrico debe ser un número positivo")
    private Long faceIdBiometrico;
}
