package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.CuentaSaldoRequest;
import com.proyecto.servicios.model.cliente.request.DepositoRequest;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.model.common.PaginaResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.PatronesValidacion;
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
 * Controlador REST para la consulta de cuentas bancarias y saldos. Los criterios se reciben en el cuerpo.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas y Saldos", description = "Búsqueda de cuentas bancarias y consulta de saldo")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (detalle por campo en 'errores')",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "401", description = "Falta el token Bearer o es inválido/expirado",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "403", description = "La cuenta no pertenece al cliente autenticado",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
})
public class CuentaController {

    private final CuentaService cuentaService;
    private final ControlAcceso controlAcceso;

    @Operation(
            summary = "Buscar cuentas por criterios combinables",
            description = "Filtros opcionales combinables: numeroCuenta, clienteId y estatus (ACTIVA o CANCELADA). "
                    + "Ej. {\"estatus\": \"ACTIVA\"} para cuentas activas. Un CLIENTE solo obtiene sus propias cuentas."
    )
    @ApiResponse(responseCode = "200", description = "Página de cuentas que cumplen los filtros (puede estar vacía)")
    @PostMapping(value = "/buscar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<PaginaResponse<CuentaResponse>>> buscarCuentas(@Valid @RequestBody CuentaBusquedaRequest request) {
        log.info("Petición POST /api/v1/cuentas/buscar");
        PaginaResponse<CuentaResponse> pagina = cuentaService.buscar(controlAcceso.limitarBusquedaCuentas(request));
        String mensaje = pagina.getTotalElementos() == 0
                ? "No se encontraron cuentas con los criterios indicados"
                : String.format("Se encontraron %d cuenta(s)", pagina.getTotalElementos());
        return ResponseEntity.ok(RespuestaApi.exito(mensaje, pagina));
    }

    @Operation(summary = "Consultar saldo de una cuenta", description = "Devuelve el desglose de saldos de la cuenta indicada en el cuerpo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo consultado"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada (solo visible para el ejecutivo)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/saldo", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<SaldoResponse>> consultarSaldo(@Valid @RequestBody CuentaSaldoRequest request) {
        log.info("Petición POST /api/v1/cuentas/saldo");
        String numeroCuenta = request.getNumeroCuenta();
        controlAcceso.validarRecursoPropio(c -> PatronesValidacion.esCuentaDe(c, numeroCuenta), "la cuenta solicitada");
        SaldoResponse saldo = cuentaService.consultarSaldoPorNumeroCuenta(numeroCuenta);
        return ResponseEntity.ok(RespuestaApi.exito("Saldo consultado exitosamente", saldo));
    }

    @Operation(summary = "Depositar saldo a una cuenta (solo ejecutivo)",
            description = "Abona saldo a una cuenta activa para que el cliente pueda realizar transacciones.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Depósito aplicado"),
            @ApiResponse(responseCode = "404", description = "Cuenta no encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
            @ApiResponse(responseCode = "422", description = "La cuenta no está activa",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
    })
    @PostMapping(value = "/deposito", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<SaldoResponse>> depositar(@Valid @RequestBody DepositoRequest request) {
        log.info("Petición POST /api/v1/cuentas/deposito");
        controlAcceso.validarRolEjecutivo();
        SaldoResponse saldo = cuentaService.depositar(request.getNumeroCuenta(), request.getMonto());
        return ResponseEntity.ok(RespuestaApi.exito("Depósito aplicado exitosamente", saldo));
    }
}
