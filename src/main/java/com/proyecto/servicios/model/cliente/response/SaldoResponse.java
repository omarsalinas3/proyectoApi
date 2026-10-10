package com.proyecto.servicios.model.cliente.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response DTO para consultas de Saldo Bancario.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de saldo disponible y retenido")
public class SaldoResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Código de respuesta estandarizado", example = "EXITO")
    @JsonProperty("codigoRespuesta")
    private CodigoRespuestaEnum codigoRespuesta;

    @Schema(description = "Código numérico (0 = Éxito)", example = "0")
    @JsonProperty("codigo")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo", example = "Saldo consultado exitosamente")
    @JsonProperty("mensaje")
    private String mensaje;

    @Schema(description = "Detalle del saldo")
    private SaldoDTO saldo;
}
