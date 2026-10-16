package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.cliente.request.LayawayClienteRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.exception.ParametroInvalidoException;
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
 * @version 1.0
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
            description = "Ejecuta el registro completo con cuenta bancaria automática (bandera 1, público), actualización de datos preservando CURP/RFC (bandera 2) o baja lógica (bandera 3). Las banderas 2 y 3 requieren el token de POST /api/v1/auth/login (botón Authorize) y solo permiten operar sobre el propio clienteId."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Operación completada exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Error de validación de negocio o bandera inválida",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Banderas 2 y 3: falta el token Bearer o es inválido/expirado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Banderas 2 y 3: el clienteId no corresponde al cliente autenticado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado para actualizar o eliminar",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto por duplicidad de CURP, RFC o correo electrónico",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(value = "/cliente", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> procesarCliente(@Valid @RequestBody LayawayClienteRequest request) {
        log.info("Petición POST /api/v1/layaway/cliente recibida con bandera: {}", request.getBandera());
        validarAccesoOperacion(request);
        ClienteResponse response = layawayClienteService.procesarOperacionCliente(request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * La bandera 1 (alta) es pública; las banderas 2 (actualizar) y 3 (baja) requieren token y solo permiten
     * operar sobre el propio cliente (o sobre cualquiera si el rol es EJECUTIVO). Se valida antes de la lógica de negocio.
     */
    private void validarAccesoOperacion(LayawayClienteRequest request) {
        Integer bandera = request.getBandera();
        if (bandera == null || (bandera != 2 && bandera != 3)) {
            return;
        }
        controlAcceso.usuarioActual();
        Long clienteId = request.getClienteId();
        if (clienteId == null || clienteId <= 0) {
            throw new ParametroInvalidoException("[clienteId] El clienteId es obligatorio y debe ser un número entero mayor a 0 para la bandera " + bandera);
        }
        controlAcceso.validarAccesoCliente(clienteId);
    }
}
