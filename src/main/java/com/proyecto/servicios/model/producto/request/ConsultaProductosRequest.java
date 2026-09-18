package com.proyecto.servicios.model.producto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Data Transfer Object (DTO) para la petición de consulta o filtrado de productos.
 *
 * <p><strong>Reglas de diseño aplicadas:</strong>
 * <ul>
 *   <li>Exclusión de nulos: {@link JsonInclude.Include#NON_NULL} garantiza que parámetros no enviados no viajen como nulos.</li>
 *   <li>Tipado numérico estricto con validaciones Jakarta: {@link Min} y {@link Max} en paginación.</li>
 *   <li>Modularidad: Ubicado en subcarpeta específica {@code model.producto.request}.</li>
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
public class ConsultaProductosRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador numérico de la categoría para filtrar (opcional).
     */
    @JsonProperty("idCategoria")
    private Integer idCategoria;

    /**
     * Estado del producto para filtrar (opcional, true=activos, false=inactivos).
     */
    @JsonProperty("activo")
    private Boolean activo;

    /**
     * Número de página para la paginación (mínimo 1).
     */
    @Min(value = 1, message = "El número de página debe ser mayor o igual a 1")
    @JsonProperty("pagina")
    private Integer pagina;

    /**
     * Límite de registros por página (mínimo 1, máximo 100).
     */
    @Min(value = 1, message = "El límite por página debe ser al menos 1")
    @Max(value = 100, message = "El límite por página no puede superar 100")
    @JsonProperty("limite")
    private Integer limite;
}
