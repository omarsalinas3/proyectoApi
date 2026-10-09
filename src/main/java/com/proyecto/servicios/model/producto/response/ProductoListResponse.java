package com.proyecto.servicios.model.producto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) de respuesta para la lista de productos obtenida del servicio externo.
 *
 * <p><strong>Convención de códigos:</strong>
 * <ul>
 *   <li>{@code codigoRespuesta = EXITO (0)} → operación exitosa.</li>
 *   <li>{@code codigoRespuesta != EXITO} → condición de error o advertencia.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoListResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Código de resultado estandarizado.
     * {@code 0 = EXITO}, valores mayores = errores o advertencias.
     *
     * @see CodigoRespuestaEnum
     */
    @JsonProperty("codigoRespuesta")
    private CodigoRespuestaEnum codigoRespuesta;

    /**
     * Código numérico de resultado (retrocompatibilidad con el proveedor externo).
     * Priorizar {@link #codigoRespuesta} para lógica de negocio interna.
     */
    @JsonProperty("codigo")
    private Integer codigo;

    /**
     * Mensaje descriptivo del resultado de la operación.
     */
    @JsonProperty("mensaje")
    private String mensaje;

    /**
     * Total de registros devueltos.
     */
    @JsonProperty("totalRegistros")
    private Long totalRegistros;

    /**
     * Fecha y hora en que se generó la respuesta.
     */
    @JsonProperty("fechaConsulta")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime fechaConsulta;

    /**
     * Origen de los datos obtenidos (SERVICIO_EXTERNO, CACHE_REDIS, BASE_DE_DATOS, SIN_DATOS).
     */
    @JsonProperty("origenDatos")
    private String origenDatos;

    /**
     * Colección de productos retornados.
     */
    @JsonProperty("productos")
    private List<ProductoItemDTO> productos;
}
