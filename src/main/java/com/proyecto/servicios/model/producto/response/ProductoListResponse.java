package com.proyecto.servicios.model.producto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
 * <p><strong>Reglas de diseño aplicadas:</strong>
 * <ul>
 *   <li>Exclusión de nulos: {@link JsonInclude.Include#NON_NULL} evita serializar propiedades no informadas.</li>
 *   <li>Tipado estricto: {@link Integer} para códigos de estado, {@link Long} para conteos y {@link LocalDateTime} para fechas.</li>
 *   <li>Modularidad: Ubicado en paquete específico de respuestas {@code model.producto.response}.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
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
     * Código de resultado de la operación (ej. 200, 0 o código de negocio).
     */
    @JsonProperty("codigo")
    private Integer codigo;

    /**
     * Mensaje descriptivo del resultado de la operación.
     */
    @JsonProperty("mensaje")
    private String mensaje;

    /**
     * Total de registros devueltos o disponibles en el sistema.
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
     * Colección de productos retornados.
     */
    @JsonProperty("productos")
    private List<ProductoItemDTO> productos;
}
