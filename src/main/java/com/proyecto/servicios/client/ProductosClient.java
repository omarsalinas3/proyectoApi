package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.ProductosFeignConfig;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Cliente HTTP declarativo OpenFeign para consumir el servicio externo de productos.
 *
 * <p><strong>Detalles de Integración:</strong>
 * <ul>
 *   <li><strong>Endpoint consumido:</strong> {@code GET /sistema/service/getProductList.do}</li>
 *   <li><strong>Autenticación:</strong> Bearer Token inyectado vía {@link ProductosFeignConfig}.</li>
 *   <li><strong>Resiliencia:</strong> Timeouts de conexión y lectura gestionados por configuración.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@FeignClient(
        name = "productosClient",
        url = "${servicio.externo.productos.url}",
        configuration = ProductosFeignConfig.class
)
public interface ProductosClient {

    /**
     * Consume el endpoint externo para obtener el listado completo de productos disponibles.
     *
     * @return {@link ProductoListResponse} con los datos tipados de los productos obtenidos.
     */
    @GetMapping(
            value = "${servicio.externo.productos.endpoint.get-products:/sistema/service/getProductList.do}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    ProductoListResponse getProductList();
}
