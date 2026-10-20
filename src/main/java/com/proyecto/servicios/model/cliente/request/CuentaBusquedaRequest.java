package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Criterios de búsqueda de cuentas bancarias. Todos los filtros son opcionales y se combinan con AND.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Filtros combinables para la búsqueda de cuentas (todos opcionales)")
public class CuentaBusquedaRequest extends RequestEstricto {

    @Schema(description = "Número de cuenta de 16 dígitos", example = "4152000000000001")
    @Pattern(regexp = PatronesValidacion.NUMERO_CUENTA, message = PatronesValidacion.MENSAJE_NUMERO_CUENTA)
    private String numeroCuenta;

    @Schema(description = "Identificador del cliente titular", example = "1")
    @Positive(message = PatronesValidacion.MENSAJE_ID)
    private Long clienteId;

    @Schema(description = "Estatus de la cuenta", example = "ACTIVA", allowableValues = {"ACTIVA", "CANCELADA"})
    @Pattern(regexp = "^(ACTIVA|CANCELADA)$", message = "El estatus debe ser ACTIVA o CANCELADA")
    private String estatus;

    @Schema(description = "Número de página (inicia en 0)", example = "0", defaultValue = "0")
    @Min(value = 0, message = "La página debe ser mayor o igual a 0")
    @Builder.Default
    private Integer pagina = 0;

    @Schema(description = "Elementos por página (1 a 100)", example = "20", defaultValue = "20")
    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = ClienteBusquedaRequest.TAMANIO_MAXIMO, message = "El tamaño de página no debe exceder " + ClienteBusquedaRequest.TAMANIO_MAXIMO)
    @Builder.Default
    private Integer tamanio = 20;
}
