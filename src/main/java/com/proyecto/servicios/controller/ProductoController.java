package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para la exposición de endpoints relacionados con el catálogo de productos.
 *
 * <p><strong>Características:</strong>
 * <ul>
 *   <li>Inyección de dependencias por constructor.</li>
 *   <li>Documentación OpenAPI 3 / Swagger integrada.</li>
 *   <li>Respuestas normalizadas en formato JSON sin atributos nulos.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Controlador para la consulta y gestión de productos integrados con servicios externos")
public class ProductoController {

    private final ProductoService productoService;

    /**
     * Constructor para la inyección de dependencias.
     *
     * @param productoService servicio de negocio de productos.
     */
    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    /**
     * Endpoint REST para consultar el catálogo completo de productos.
     *
     * @return {@link ResponseEntity} con el {@link ProductoListResponse} y código HTTP 200 OK.
     */
    @Operation(
            summary = "Consultar lista de productos",
            description = "Invoca el servicio externo configurado mediante Bearer Token para obtener el listado de productos disponibles."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Lista de productos obtenida exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProductoListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado - Fallo de autenticación con el servicio externo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Bad Gateway - Error devuelto por el servicio externo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "504",
                    description = "Gateway Timeout - Tiempo de espera agotado al comunicar con el servicio externo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoListResponse> obtenerListaProductos() {
        log.info("Recibida petición HTTP GET /api/v1/productos");
        ProductoListResponse response = productoService.consultarListaProductos();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
