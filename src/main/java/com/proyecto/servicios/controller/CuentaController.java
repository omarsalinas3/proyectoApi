package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.PatronesValidacion;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para la consulta de cuentas bancarias y verificación de saldos.
 *
 * <p><strong>Endpoints expuestos:</strong>
 * <ul>
 *   <li>{@code GET /api/v1/cuentas/{numeroCuenta}} - Consulta el detalle de una cuenta por su número de 16 dígitos.</li>
 *   <li>{@code GET /api/v1/cuentas/activas} - Listado de todas las cuentas bancarias en estado ACTIVA.</li>
 *   <li>{@code GET /api/v1/cuentas/{numeroCuenta}/saldo} - Consulta de saldos contables y disponibles de la cuenta.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@io.swagger.v3.oas.annotations.Hidden
@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
@Tag(name = "Cuentas y Saldos", description = "Controlador para la gestión y consulta de Cuentas Bancarias y Saldos")
public class CuentaController {

    private final CuentaService cuentaService;
    private final ControlAcceso controlAcceso;

    @Operation(summary = "Consultar cuenta por número de 16 dígitos", description = "Recupera los datos de la cuenta bancaria y su titular.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cuenta bancaria encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = CuentaResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta no encontrada",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(value = "/{numeroCuenta}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(
            @Parameter(description = "Número de cuenta de 16 dígitos", example = "1234567890123456")
            @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
            @PathVariable String numeroCuenta) {
        log.info("Petición GET /api/v1/cuentas/{}", numeroCuenta);
        controlAcceso.validarRecursoPropio(c -> PatronesValidacion.esCuentaDe(c, numeroCuenta), "la cuenta");
        CuentaResponse response = cuentaService.consultarPorNumeroCuenta(numeroCuenta);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar cuentas activas", description = "Retorna el listado de todas las cuentas bancarias actualmente activas en el sistema.")
    @ApiResponse(
            responseCode = "200",
            description = "Listado de cuentas activas",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CuentaResponse.class)))
    )
    @GetMapping(value = "/activas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CuentaResponse>> consultarCuentasActivas() {
        log.info("Petición GET /api/v1/cuentas/activas");
        controlAcceso.validarRolEjecutivo();
        List<CuentaResponse> response = cuentaService.consultarCuentasActivas();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Consultar saldo de cuenta", description = "Obtiene el desglose de saldos (disponible, contable, bloqueado, pendiente) de la cuenta.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Saldo de cuenta consultado exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = SaldoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cuenta o registro de saldos no encontrado",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(value = "/{numeroCuenta}/saldo", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SaldoResponse> consultarSaldoPorNumeroCuenta(
            @Parameter(description = "Número de cuenta de 16 dígitos", example = "1234567890123456")
            @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
            @PathVariable String numeroCuenta) {
        log.info("Petición GET /api/v1/cuentas/{}/saldo", numeroCuenta);
        controlAcceso.validarRecursoPropio(c -> PatronesValidacion.esCuentaDe(c, numeroCuenta), "la cuenta");
        SaldoResponse response = cuentaService.consultarSaldoPorNumeroCuenta(numeroCuenta);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
