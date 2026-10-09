package com.proyecto.servicios.model.producto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request DTO para el endpoint POST de consulta de productos en caché.
 *
 * <p>Todos los campos son opcionales. Si se envían, actúan como filtros
 * sobre el catálogo almacenado en Redis o PostgreSQL.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Parámetros opcionales para filtrar el catálogo de productos almacenado en caché.")
public class ProductoConsultaRequest {

    @Schema(description = "Filtrar por tipo de presentación en front (1=tarjeta, 2=pago servicio, 3=otro)", example = "1")
    private Integer tipoFront;

    @Schema(description = "Filtrar por nombre de servicio (búsqueda parcial, case-insensitive)", example = "Recorcholis")
    private String servicio;

    @Schema(description = "Filtrar por nombre de producto (búsqueda parcial, case-insensitive)", example = "Recarga")
    private String producto;
}
