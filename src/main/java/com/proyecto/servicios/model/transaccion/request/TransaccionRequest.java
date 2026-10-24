package com.proyecto.servicios.model.transaccion.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Petición de pago/venta ({@code POST /api/v1/transacciones}). Longitudes según la documentación de GestoPago:
 * teléfono 10-20 y referencia 1-100 caracteres.
 *
 * <ul>
 *   <li>Recarga de tiempo aire / paquetes: {@code telefono} (el número a recargar).</li>
 *   <li>Pago de servicio (luz, agua, gas, teléfono fijo...): {@code referencia} del recibo.</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Datos de la transacción a enviar a GestoPago")
public class TransaccionRequest extends RequestEstricto {

    @Schema(description = "Número de cuenta de 16 dígitos desde la que se paga", example = "4152262623176650",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
    private String numCuenta;

    @Schema(description = "idServicio del catálogo de GestoPago (GET /api/v1/productos)", example = "133",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El idServicio es obligatorio")
    @Positive(message = "El idServicio debe ser un número entero mayor a 0")
    private Integer idServicio;

    @Schema(description = "idProducto del catálogo de GestoPago (GET /api/v1/productos)", example = "582",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El idProducto es obligatorio")
    @Positive(message = "El idProducto debe ser un número entero mayor a 0")
    private Long idProducto;

    @Schema(description = "Monto de la operación; en productos de precio fijo debe ser igual a su precio", example = "10.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    @DecimalMax(value = "99999.99", message = "El monto no debe exceder 99,999.99")
    @Digits(integer = 5, fraction = 2, message = "El monto debe tener como máximo 2 decimales")
    private BigDecimal monto;

    @Schema(description = "Recargas: número telefónico a recargar (10 a 20 dígitos). No aplica en pagos de servicios",
            example = "5555555551")
    @Pattern(regexp = "^\\d{10,20}$", message = "El teléfono debe contener entre 10 y 20 dígitos (sin espacios ni guiones)")
    private String telefono;

    @Schema(description = "Pagos de servicios (luz, agua, teléfono fijo...): referencia impresa en el recibo (1 a 100 "
            + "caracteres alfanuméricos). No aplica en recargas", example = "25720381")
    @Pattern(regexp = "^[A-Za-z0-9]{1,100}$", message = "La referencia debe tener entre 1 y 100 caracteres alfanuméricos (sin espacios)")
    private String referencia;

    @Schema(description = "Código de 6 dígitos de la app de autenticación (2FA). También se acepta como \"2fa\"",
            example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonAlias("2fa")
    @NotBlank(message = "El código de verificación (2FA) es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación (2FA) debe contener exactamente 6 dígitos")
    @ToString.Exclude
    private String codigo2fa;

    /**
     * Se exige el dato que corresponde al producto (teléfono en recargas, referencia en pagos de servicios); cuál de
     * los dos se valida contra el catálogo al armar la transacción.
     */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Indique el teléfono (recargas) o la referencia del recibo (pagos de servicios como luz o agua)")
    public boolean isTelefonoOReferencia() {
        return (telefono != null && !telefono.isBlank()) || (referencia != null && !referencia.isBlank());
    }
}
