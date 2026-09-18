package com.proyecto.servicios.service;

import com.proyecto.servicios.model.producto.response.ProductoListResponse;

/**
 * Interfaz de servicio de negocio para la gestión y consulta de productos externos.
 *
 * <p>Define las operaciones disponibles para interactuar con la información de productos
 * obtenida mediante la integración con el servicio externo.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
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
}
