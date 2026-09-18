package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.ProductosFeignConfig;
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
 *   <li><strong>Formatos soportados:</strong> XML y JSON.</li>
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
     * Consume el endpoint externo para obtener el contenido en crudo (XML o JSON).
     *
     * @return {@link String} con el payload en crudo retornado por el servicio externo.
     */
    @GetMapping(
            value = "${servicio.externo.productos.endpoint.get-products:/sistema/service/getProductList.do}",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_XML_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    String getProductListRaw();
}
