package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controlador REST para el Onboarding, consulta, modificación y baja lógica de Clientes Personas Físicas.
 *
 * <p><strong>Endpoints expuestos:</strong>
 * <ul>
 *   <li>{@code POST /api/v1/clientes} - Registro / Onboarding de cliente y creación automática de cuenta bancaria.</li>
 *   <li>{@code PUT /api/v1/clientes/{id}} - Actualización de datos permitidos protegiendo inmutables (CURP, RFC, Cuentas).</li>
 *   <li>{@code DELETE /api/v1/clientes/{id}} - Baja lógica del cliente y cancelación de cuentas.</li>
 *   <li>{@code GET /api/v1/clientes/{id}} - Consulta de cliente por ID.</li>
 *   <li>{@code GET /api/v1/clientes/curp/{curp}} - Consulta por CURP.</li>
 *   <li>{@code GET /api/v1/clientes/rfc/{rfc}} - Consulta por RFC.</li>
 *   <li>{@code GET /api/v1/clientes/correo/{correo}} - Consulta por correo electrónico.</li>
 *   <li>{@code GET /api/v1/clientes/cuenta/{numeroCuenta}} - Consulta por número de cuenta de 16 dígitos.</li>
 *   <li>{@code GET /api/v1/clientes} - Listado general de clientes con filtro de activos.</li>
 *   <li>{@code GET /api/v1/clientes/rango-fechas} - Consulta por rango de fechas de registro.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@io.swagger.v3.oas.annotations.Hidden
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Controlador para el Onboarding y ciclo de vida de Clientes Personas Físicas")
public class ClienteController {

    private final ClienteService clienteService;

    @Operation(
            summary = "Registrar nuevo cliente (Onboarding)",
            description = "Registra un cliente persona física mayor de edad, validando unicidad de CURP, RFC, correo y usuario. Genera automáticamente una cuenta de débito de 16 dígitos con saldo inicial de $0.00 y credenciales cifradas."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Cliente registrado y cuenta bancaria aperturada exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos de entrada inválidos o reglas de validación insatisfechas",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflicto por duplicidad de CURP, RFC, correo o usuario",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Infracción de reglas de negocio (ej. menor de 18 años)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        log.info("Petición POST /api/v1/clientes para CURP: {}", request.getCurp());
        ClienteResponse response = clienteService.registrarCliente(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(
            summary = "Actualizar datos del cliente",
            description = "Actualiza información editable del cliente (nombres, contacto, ocupación, domicilio). Los campos CURP, RFC y número de cuenta son inmutables."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente actualizado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El nuevo correo ya se encuentra en uso por otro cliente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "Intento de modificación de campos inmutables (CURP o RFC)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizarCliente(
            @Parameter(description = "Identificador único del cliente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequest request) {
        log.info("Petición PUT /api/v1/clientes/{}", id);
        ClienteResponse response = clienteService.actualizarCliente(id, request);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(
            summary = "Baja lógica del cliente",
            description = "Cambia el estado del cliente a inactivo (activo = false), cancela su cuenta bancaria y desactiva su sesión."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente dado de baja lógica correctamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> eliminarClienteLogico(
            @Parameter(description = "Identificador único del cliente", example = "1") @PathVariable Long id) {
        log.info("Petición DELETE /api/v1/clientes/{}", id);
        ClienteResponse response = clienteService.eliminarClienteLogico(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cliente por ID", description = "Recupera los datos del cliente, su domicilio, cuenta y saldo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Long id) {
        log.info("Petición GET /api/v1/clientes/{}", id);
        ClienteResponse response = clienteService.consultarPorId(id);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cliente por CURP", description = "Busca un cliente a partir de su clave oficial CURP.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/curp/{curp}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCurp(@PathVariable String curp) {
        log.info("Petición GET /api/v1/clientes/curp/{}", curp);
        ClienteResponse response = clienteService.consultarPorCurp(curp);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cliente por RFC", description = "Busca un cliente a partir de su Registro Federal de Contribuyentes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/rfc/{rfc}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorRfc(@PathVariable String rfc) {
        log.info("Petición GET /api/v1/clientes/rfc/{}", rfc);
        ClienteResponse response = clienteService.consultarPorRfc(rfc);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cliente por correo", description = "Busca un cliente a partir de su correo electrónico.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/correo/{correo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorCorreo(@PathVariable String correo) {
        log.info("Petición GET /api/v1/clientes/correo/{}", correo);
        ClienteResponse response = clienteService.consultarPorCorreo(correo);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cliente por número de cuenta", description = "Busca el cliente titular de una cuenta de 16 dígitos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ClienteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping(value = "/cuenta/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        log.info("Petición GET /api/v1/clientes/cuenta/{}", numeroCuenta);
        ClienteResponse response = clienteService.consultarPorNumeroCuenta(numeroCuenta);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar todos los clientes", description = "Retorna el listado general de clientes, con filtro opcional de solo activos.")
    @ApiResponse(
            responseCode = "200",
            description = "Listado de clientes",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class)))
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarTodos(
            @Parameter(description = "Filtrar únicamente clientes activos") @RequestParam(required = false) Boolean soloActivos) {
        log.info("Petición GET /api/v1/clientes (soloActivos: {})", soloActivos);
        List<ClienteResponse> response = clienteService.consultarTodos(soloActivos);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar clientes por rango de fechas de registro", description = "Retorna los clientes registrados entre dos fechas (ISO: YYYY-MM-DD).")
    @ApiResponse(
            responseCode = "200",
            description = "Listado de clientes en el rango",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = ClienteResponse.class)))
    )
    @GetMapping(value = "/rango-fechas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ClienteResponse>> consultarPorRangoFechas(
            @Parameter(description = "Fecha de inicio (YYYY-MM-DD)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @Parameter(description = "Fecha de fin (YYYY-MM-DD)", example = "2026-12-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        log.info("Petición GET /api/v1/clientes/rango-fechas ({} al {})", inicio, fin);
        List<ClienteResponse> response = clienteService.consultarPorRangoFechas(inicio, fin);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
