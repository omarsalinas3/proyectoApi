package com.proyecto.servicios.model.producto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contenedor de respuesta para productos agrupados por su identificador de presentación {@code tipoFront}.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta con los productos clasificados por tipoFront mediante Streams y Predicates")
public class ProductoAgrupadoResponse {

    @Schema(description = "Código de resultado estandarizado (0=éxito, >0=error/advertencia)", example = "EXITO")
    @JsonProperty("codigoRespuesta")
    private CodigoRespuestaEnum codigoRespuesta;

    @Schema(description = "Código numérico del resultado (retrocompatibilidad con proveedor)", example = "0")
    @JsonProperty("codigo")
    private Integer codigo;

    @Schema(description = "Mensaje descriptivo del resultado", example = "Productos agrupados exitosamente")
    @JsonProperty("mensaje")
    private String mensaje;

    @Schema(description = "Origen de los datos recuperados", example = "SERVICIO_EXTERNO")
    @JsonProperty("origenDatos")
    private String origenDatos;

    @Schema(description = "Total general de registros en todos los grupos", example = "900")
    @JsonProperty("totalRegistros")
    private Long totalRegistros;

    @Schema(description = "Fecha y hora de la consulta")
    @JsonProperty("fechaConsulta")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaConsulta;

    @Schema(description = "Mapa de productos agrupados por tipoFront (ej. 1: Recargas, 2: Servicios, 3: Pines)")
    @JsonProperty("grupos")
    @Builder.Default
    private Map<Integer, List<ProductoItemDTO>> grupos = new HashMap<>();
}
