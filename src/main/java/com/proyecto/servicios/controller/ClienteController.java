package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.ClienteIdRequest;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.model.common.PaginaResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.ClienteService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la administración de clientes (usuarios personas físicas).
 *
 * <p>Toda la información se recibe en el cuerpo de la petición mediante Request DTO validados con
 * {@code @Valid}; los errores de validación y de negocio los procesa {@code GlobalExceptionHandler}.
 * Las consultas por cualquier criterio se resuelven en un único endpoint de búsqueda con filtros combinables.</p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Alta, búsqueda, actualización y baja lógica de clientes personas físicas")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (detalle por campo en 'errores')",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "401", description = "Falta el token Bearer o es inválido/expirado",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "403", description = "El cliente autenticado intenta operar sobre otro cliente",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
})
public class ClienteController {

    private final ClienteService clienteService;
    private final ControlAcceso controlAcceso;

    @Operation(
            summary = "Registrar cliente (onboarding, público)",
            description = "Registra un cliente mayor de edad validando unicidad de CURP, RFC, correo y usuario. Genera su cuenta "
                    + "de 16 dígitos con saldo inicial 0.00 y estatus ACTIVA. Los campos controlados por el servidor (id, activo, "
                    + "cuenta, saldos, fechas) no se aceptan en la petición."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado y cuenta aperturada"),
            @ApiResponse(responseCode = "409", description = "CURP, RFC, correo o usuario ya registrados",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida (ej. menor de 18 años)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<ClienteResponse>> registrarCliente(@Valid @RequestBody ClienteRegistroRequest request) {
        log.info("Petición POST /api/v1/clientes");
        ClienteResponse cliente = clienteService.registrarCliente(request);
        return new ResponseEntity<>(RespuestaApi.exito("Cliente y cuenta bancaria registrados exitosamente", cliente), HttpStatus.CREATED);
    }

    @Operation(
            summary = "Buscar clientes por criterios combinables",
            description = "Único endpoint de consulta. Todos los filtros son opcionales y se combinan (AND): id, curp, rfc, correo, "
                    + "numeroCuenta, nombre (contenido en nombre o apellidos), activo y rango de fechas de registro. Sin filtros "
                    + "devuelve todos los clientes paginados. Un CLIENTE solo obtiene su propio registro; el EJECUTIVO, cualquiera."
    )
    @ApiResponse(responseCode = "200", description = "Página de clientes que cumplen los filtros (puede estar vacía)")
    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<PaginaResponse<ClienteResponse>>> buscarClientes(@Valid @RequestBody ClienteBusquedaRequest request) {
        log.info("Petición POST /api/v1/clientes/buscar");
        PaginaResponse<ClienteResponse> pagina = clienteService.buscar(controlAcceso.limitarBusquedaClientes(request));
        String mensaje = pagina.getTotalElementos() == 0
                ? "No se encontraron clientes con los criterios indicados"
                : String.format("Se encontraron %d cliente(s)", pagina.getTotalElementos());
        return ResponseEntity.ok(RespuestaApi.exito(mensaje, pagina));
    }

    @Operation(
            summary = "Actualizar datos del cliente",
            description = "Actualiza datos personales, de contacto, domicilio e información laboral del cliente indicado en 'id'. "
                    + "CURP, RFC, número de cuenta, saldos, estatus y fechas no se pueden modificar: si se envían se responde 400."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente actualizado"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado (solo visible para el ejecutivo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "409", description = "El nuevo correo pertenece a otro cliente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<ClienteResponse>> actualizarCliente(@Valid @RequestBody ClienteUpdateRequest request) {
        log.info("Petición PUT /api/v1/clientes (id: {})", request.getId());
        controlAcceso.validarAccesoCliente(request.getId());
        ClienteResponse cliente = clienteService.actualizarCliente(request);
        return ResponseEntity.ok(RespuestaApi.exito("Datos del cliente actualizados exitosamente", cliente));
    }

    @Operation(
            summary = "Baja lógica del cliente",
            description = "Marca al cliente como inactivo, cancela su cuenta y cierra su sesión. La información no se elimina físicamente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente dado de baja"),
            @ApiResponse(responseCode = "404", description = "Cliente no encontrado (solo visible para el ejecutivo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<ClienteResponse>> eliminarClienteLogico(@Valid @RequestBody ClienteIdRequest request) {
        log.info("Petición DELETE /api/v1/clientes (id: {})", request.getId());
        controlAcceso.validarAccesoCliente(request.getId());
        ClienteResponse cliente = clienteService.eliminarClienteLogico(request.getId());
        return ResponseEntity.ok(RespuestaApi.exito("Cliente dado de baja y cuenta cancelada correctamente", cliente));
    }
}
