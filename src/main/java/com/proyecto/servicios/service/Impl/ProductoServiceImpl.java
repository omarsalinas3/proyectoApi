package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.ProductosClient;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementación de la capa de servicio de negocio {@link ProductoService}.
 *
 * <p><strong>Principios aplicados:</strong>
 * <ul>
 *   <li>Inyección de dependencias por constructor: Inmutabilidad y facilidad de pruebas unitarias.</li>
 *   <li>Registro y monitoreo con SLF4J: Registro del inicio, tiempo transcurrido (ms) y fin de la invocación.</li>
 *   <li>Seguridad: Ningún token o dato sensible es expuesto en las trazas de log.</li>
 *   <li>Limpieza y robustez: Manejo defensivo ante respuestas nulas o inconsistentes.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductosClient productosClient;

    /**
     * Constructor para la inyección de dependencias.
     *
     * @param productosClient cliente Feign para la integración externa de productos.
     */
    public ProductoServiceImpl(ProductosClient productosClient) {
        this.productosClient = productosClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductoListResponse consultarListaProductos() {
        log.info("Iniciando consumo de servicio externo para obtener lista de productos [GET /sistema/service/getProductList.do]");
        long tiempoInicio = System.currentTimeMillis();

        try {
            // Invocación al cliente Feign de integración
            ProductoListResponse response = productosClient.getProductList();

            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            if (response == null) {
                log.warn("El servicio externo retornó una respuesta nula tras {} ms", tiempoTotal);
                return ProductoListResponse.builder()
                        .codigo(204)
                        .mensaje("No se obtuvo contenido del servicio externo")
                        .fechaConsulta(LocalDateTime.now())
                        .build();
            }

            int totalProductos = (response.getProductos() != null) ? response.getProductos().size() : 0;
            log.info("Consumo de servicio externo finalizado exitosamente en {} ms. Total de productos obtenidos: {}", 
                    tiempoTotal, totalProductos);

            // Asignar fecha de consulta si no viene provista
            if (response.getFechaConsulta() == null) {
                response.setFechaConsulta(LocalDateTime.now());
            }

            return response;

        } catch (Exception ex) {
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            log.error("Fallo durante el consumo del servicio externo tras {} ms. Causa: {}", tiempoTotal, ex.getMessage());
            // Se propaga la excepción tipada para ser capturada por el GlobalExceptionHandler
            throw ex;
        }
    }
}
