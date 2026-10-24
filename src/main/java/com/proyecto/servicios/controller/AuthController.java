package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.request.DosFactoresCodigoRequest;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.request.VerificarSesionRequest;
import com.proyecto.servicios.model.cliente.response.DosFactoresResponse;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.service.DosFactoresService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de autenticación: inicio de sesión, verificación y cierre de la sesión del usuario del token.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión con contraseña o Face ID, verificación y cierre de sesión")
public class AuthController {

    private final AuthService authService;
    private final DosFactoresService dosFactoresService;
    private final ControlAcceso controlAcceso;

    @Operation(
            summary = "Inicio de sesión (Bandera 1 = Contraseña, Bandera 2 = Biométrico Face ID)",
            description = "Autentica con usuario/correo y contraseña, o con Face ID. Devuelve el token JWT que debe enviarse en "
                    + "'Authorization: Bearer <token>' (botón Authorize) en el resto de operaciones."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión iniciada; el token viene en datos.token"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (detalle por campo en 'errores')",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "401", description = "Usuario o credenciales incorrectas, o cliente dado de baja",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "422", description = "Falta la contraseña (bandera 1) o el Face ID (bandera 2)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        log.info("Petición POST /api/v1/auth/login para usuario: {}, bandera: {}", request.getUsername(), request.getBandera());
        LoginResponse sesion = authService.login(request);
        return ResponseEntity.ok(RespuestaApi.exito("Inicio de sesión exitoso", sesion));
    }

    @Operation(
            summary = "Verificar la sesión del usuario autenticado",
            description = "Verifica la sesión del dueño del token y renueva su actividad. El cuerpo es opcional: "
                    + "{\"timeoutDinamico\": 300} evalúa un límite de inactividad distinto en esta consulta."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión activa"),
            @ApiResponse(responseCode = "401", description = "Sin token, token inválido o sesión expirada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/sesion/verificar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<LoginResponse>> verificarSesion(
            @Valid @RequestBody(required = false) VerificarSesionRequest request) {
        Integer timeoutDinamico = request != null ? request.getTimeoutDinamico() : null;
        LoginResponse sesion = authService.verificarEstadoSesion(controlAcceso.usuarioActual(), timeoutDinamico);
        return ResponseEntity.ok(RespuestaApi.exito("Sesión activa y vigente", sesion));
    }

    @Operation(
            summary = "Cerrar la sesión del usuario autenticado",
            description = "Cierra la sesión del dueño del token; a partir de ese momento el token deja de ser aceptado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sesión cerrada"),
            @ApiResponse(responseCode = "401", description = "Sin token o token inválido",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<LoginResponse>> logout() {
        LoginResponse sesion = authService.logout(controlAcceso.usuarioActual());
        return ResponseEntity.ok(RespuestaApi.exito("Cierre de sesión exitoso", sesion));
    }

    @Operation(
            summary = "Iniciar la activación del segundo factor (2FA)",
            description = "Genera el secreto TOTP del cliente. Agréguelo a Google Authenticator / Microsoft Authenticator "
                    + "escaneando la URI como QR o capturando el secreto, y luego confirme con POST /api/v1/auth/2fa/confirmar. "
                    + "El código de 6 dígitos que muestre la app se envía en cada transacción."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secreto generado; el 2FA queda inactivo hasta confirmarlo"),
            @ApiResponse(responseCode = "422", description = "El 2FA ya está activo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/2fa/activar", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<DosFactoresResponse>> activarDosFactores() {
        DosFactoresResponse dosFactores = dosFactoresService.iniciarActivacion(controlAcceso.usuarioActual());
        return ResponseEntity.ok(RespuestaApi.exito(
                "Agregue el secreto a su app de autenticación y confirme con el código que muestre", dosFactores));
    }

    @Operation(summary = "Confirmar la activación del segundo factor (2FA)",
            description = "Activa el 2FA tras validar un código vigente generado con el secreto recién entregado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "2FA activado"),
            @ApiResponse(responseCode = "403", description = "Código incorrecto o expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/2fa/confirmar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<DosFactoresResponse>> confirmarDosFactores(@Valid @RequestBody DosFactoresCodigoRequest request) {
        DosFactoresResponse dosFactores = dosFactoresService.confirmarActivacion(controlAcceso.usuarioActual(), request.getCodigo());
        return ResponseEntity.ok(RespuestaApi.exito("Segundo factor activado; ya puede realizar transacciones", dosFactores));
    }
}
