package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.request.LayawayClienteRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.LayawayClienteService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la gestión unificada de clientes mediante banderas de operación.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/layaway")
@RequiredArgsConstructor
@Tag(name = "Layaway Clientes", description = "Endpoint unificado para gestión de clientes mediante banderas de operación")
public class LayawayClienteController {

    private final LayawayClienteService layawayClienteService;
    private final ControlAcceso controlAcceso;

    @Operation(
            summary = "Operación unificada de clientes (Bandera 1 = Insertar, Bandera 2 = Actualizar, Bandera 3 = Eliminar/Baja Lógica)",
            description = "Ejecuta el registro completo con cuenta bancaria automática (bandera 1, público), actualización de datos "
                    + "preservando CURP/RFC (bandera 2) o baja lógica (bandera 3). Las banderas 2 y 3 requieren el token de "
                    + "POST /api/v1/auth/login (botón Authorize) y solo permiten operar sobre el propio clienteId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Bandera 1: cliente registrado y cuenta aperturada"),
            @ApiResponse(responseCode = "200", description = "Banderas 2 y 3: cliente actualizado o dado de baja"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (detalle por campo en 'errores')",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "401", description = "Banderas 2 y 3: falta el token Bearer o es inválido/expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "403", description = "Banderas 2 y 3: el clienteId no corresponde al cliente autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado para actualizar o eliminar",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "409", description = "CURP, RFC o correo ya registrados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida (ej. menor de 18 años)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/cliente", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<ClienteResponse>> procesarCliente(@Valid @RequestBody LayawayClienteRequest request) {
        int bandera = request.getBandera();
        log.info("Petición POST /api/v1/layaway/cliente recibida con bandera: {}", bandera);
        if (bandera != 1) {
            // Actualizar o dar de baja exige token y que el clienteId sea el del usuario autenticado
            controlAcceso.validarAccesoCliente(request.getClienteId());
        }
        ClienteResponse cliente = layawayClienteService.procesarOperacionCliente(request);
        return switch (bandera) {
            case 1 -> new ResponseEntity<>(RespuestaApi.exito(
                    "Cliente registrado y cuenta bancaria generada exitosamente (Bandera 1)", cliente), HttpStatus.CREATED);
            case 2 -> ResponseEntity.ok(RespuestaApi.exito(
                    "Cliente actualizado exitosamente preservando CURP/RFC (Bandera 2)", cliente));
            default -> ResponseEntity.ok(RespuestaApi.exito(
                    "Cliente dado de baja lógica exitosamente (Bandera 3)", cliente));
        };
    }
}
