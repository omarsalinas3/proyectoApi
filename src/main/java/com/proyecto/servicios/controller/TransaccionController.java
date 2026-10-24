package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.model.transaccion.request.ConfirmarTransaccionRequest;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.TransaccionService;
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
 * Controlador REST de transacciones (pagos de servicios, recargas y PINes) procesadas por GestoPago.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Tag(name = "Transacciones", description = "Pago de servicios, recargas y PINes con GestoPago (sendTx / confirmTx)")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos (detalle por campo en 'errores')",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "401", description = "Falta el token Bearer o es inválido/expirado",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "403", description = "La cuenta no pertenece al cliente o el código 2FA no es válido",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "422", description = "Cliente/cuenta inactivos, saldo insuficiente, 2FA no activado, "
                + "producto no disponible o transacción rechazada por GestoPago",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "502", description = "No fue posible comunicarse o autenticarse con GestoPago",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class))),
        @ApiResponse(responseCode = "504", description = "GestoPago no confirmó el resultado; consulte /confirmar con el UPC",
                content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RespuestaApi.class)))
})
public class TransaccionController {

    private final TransaccionService transaccionService;
    private final ControlAcceso controlAcceso;

    @Operation(
            summary = "Enviar transacción (sendTx)",
            description = "Valida cliente activo, saldo y código 2FA; arma el payload de GestoPago, lo cifra con AES-256, "
                    + "obtiene el token vigente, envía la transacción y, si es exitosa, carga el monto a la cuenta. "
                    + "Requiere tener activado el 2FA (POST /api/v1/auth/2fa/activar y /confirmar)."
    )
    @ApiResponse(responseCode = "201", description = "Pago exitoso: transacción aplicada y monto cargado")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<TransaccionResponse>> enviar(@Valid @RequestBody TransaccionRequest request) {
        log.info("Petición POST /api/v1/transacciones (producto {}, servicio {})", request.getIdProducto(), request.getIdServicio());
        TransaccionResponse transaccion = transaccionService.enviar(request, controlAcceso.usuarioActual());
        return new ResponseEntity<>(RespuestaApi.exito("¡Pago exitoso! " + transaccion.getMensajeProveedor(), transaccion),
                HttpStatus.CREATED);
    }

    @Operation(
            summary = "Confirmar transacción pendiente (confirmTx)",
            description = "Consulta en GestoPago el resultado de una transacción que quedó pendiente por timeout. Solo se "
                    + "permite 62 segundos después del envío y dentro de las 24 horas siguientes."
    )
    @ApiResponse(responseCode = "200", description = "La transacción fue aplicada y el monto se cargó")
    @PostMapping(value = "/confirmar", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<TransaccionResponse>> confirmar(@Valid @RequestBody ConfirmarTransaccionRequest request) {
        log.info("Petición POST /api/v1/transacciones/confirmar (upc {})", request.getUpc());
        TransaccionResponse transaccion = transaccionService.confirmar(request.getUpc(), controlAcceso.usuarioActual());
        return ResponseEntity.ok(RespuestaApi.exito("La transacción fue aplicada exitosamente", transaccion));
    }
}
