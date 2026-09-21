package com.proyecto.servicios.service;

import com.proyecto.servicios.model.producto.request.ProductoConsultaRequest;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;

/**
 * Interfaz de servicio de negocio para la gestión y consulta de productos externos.
 *
 * <p>Define las operaciones disponibles para interactuar con la información de productos
 * obtenida mediante la integración con el servicio externo.</p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
public interface ProductoService {

    /**
     * Obtiene el listado completo de productos consultando el servicio externo configurado.
     *
     * @return {@link ProductoListResponse} con la lista de productos y metadatos asociados.
     * @throws com.proyecto.servicios.exception.ExternalServiceAuthException si falla la autenticación con el servicio externo.
     * @throws com.proyecto.servicios.exception.ExternalServiceTimeoutException si se agota el tiempo de espera.
     * @throws com.proyecto.servicios.exception.ExternalServiceException si ocurre un error general en el servicio externo.
     */
    ProductoListResponse consultarListaProductos();

    /**
     * Obtiene los productos clasificados y agrupados por su tipo de presentación {@code tipoFront}
     * aplicando evaluación funcional mediante Java {@link java.util.stream.Stream} y {@link java.util.function.Predicate}.
     *
     * @return {@link com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse} con el catálogo agrupado.
     */
    com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse obtenerProductosAgrupadosPorTipoFront();

    /**
     * Consulta el catálogo de productos <strong>exclusivamente desde caché</strong>.
     *
     * <p><strong>Flujo de resiliencia:</strong>
     * <ol>
     *   <li>Consulta Redis → si existe la llave, devuelve el catálogo filtrado.</li>
     *   <li>Si Redis no tiene datos → consulta PostgreSQL como segundo nivel.</li>
     *   <li>Si ambas fuentes están vacías → devuelve respuesta con lista vacía.</li>
     * </ol>
     * </p>
     *
     * <p>Este método <strong>nunca llama al servicio externo</strong>.
     * Es ideal para búsquedas internas o consultas frecuentes sin impactar al proveedor.</p>
     *
     * @param request filtros opcionales de búsqueda (tipoFront, servicio, producto).
     * @return {@link ProductoListResponse} con el catálogo filtrado y el origen de datos.
     */
    ProductoListResponse consultarDesdeCacheOBaseDeDatos(ProductoConsultaRequest request);
}
