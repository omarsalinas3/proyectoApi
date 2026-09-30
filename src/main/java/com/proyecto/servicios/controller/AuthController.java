package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la autenticación de usuarios.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoint de inicio de sesión general con soporte para contraseña y Face ID biométrico")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Inicio de sesión de usuario (Bandera 1 = Contraseña, Bandera 2 = Biométrico Face ID)",
            description = "Permite autenticar usuarios mediante usuario/contraseña o mediante Face ID biométrico (Long). El nombre de usuario es siempre obligatorio."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = LoginResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error de validación o parámetros faltantes",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Credenciales incorrectas o biometría no coincidente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Petición POST /api/v1/auth/login para usuario: {}, bandera: {}", request.getUsername(), request.getBandera());
        LoginResponse response = authService.login(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Hidden
    @GetMapping(value = "/sesion/verificar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> verificarSesion(
            @RequestParam String usuario,
            @RequestParam(required = false) Integer timeoutDinamico) {
        LoginResponse response = authService.verificarEstadoSesion(usuario, timeoutDinamico);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Hidden
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> logout(@RequestParam String usuario) {
        LoginResponse response = authService.logout(usuario);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
