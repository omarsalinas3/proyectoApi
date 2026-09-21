package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.ErrorResponse;
import com.proyecto.servicios.model.producto.request.ProductoConsultaRequest;
import com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * Endpoint REST para consultar los productos clasificados y agrupados por su tipo de presentación en front.
     *
     * @return {@link ResponseEntity} con el {@link com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse} y código HTTP 200 OK.
     */
    @Operation(
            summary = "Consultar productos agrupados por tipoFront",
            description = "Clasifica el catálogo de productos por tipo de presentación (tipoFront 1, 2, 3...) mediante evaluación funcional con Java Streams y Predicates."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo agrupado por tipoFront obtenido exitosamente",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autorizado - Fallo de autenticación",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "Bad Gateway - Fallo en integración",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping(value = "/agrupados-por-tipo-front", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoAgrupadoResponse> obtenerProductosAgrupadosPorTipoFront() {
        log.info("Recibida petición HTTP GET /api/v1/productos/agrupados-por-tipo-front");
        ProductoAgrupadoResponse response = productoService.obtenerProductosAgrupadosPorTipoFront();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Endpoint POST para consultar el catálogo de productos desde caché (Redis o PostgreSQL).
     *
     * <p><strong>Flujo:</strong>
     * <ol>
     *   <li>Consulta Redis: si hay datos en la llave {@code catalogo:productos} → retorna filtrado.</li>
     *   <li>Consulta PostgreSQL: si Redis está vacío → lee desde la tabla {@code productos_catalogo}.</li>
     *   <li>Sin datos: devuelve lista vacía con mensaje explicativo.</li>
     * </ol>
     * </p>
     *
     * @param request filtros opcionales: {@code tipoFront}, {@code servicio}, {@code producto}.
     * @return {@link ResponseEntity} con el {@link ProductoListResponse} filtrado.
     */
    @Operation(
            summary = "Consultar catálogo desde caché (Redis → PostgreSQL)",
            description = "Consulta el catálogo almacenado. Primero revisa Redis; si no hay datos, consulta PostgreSQL. " +
                    "Nunca llama al servicio externo. Soporta filtros opcionales por tipoFront, servicio y producto."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo recuperado exitosamente desde caché (Redis o PostgreSQL)",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductoListResponse.class))
            ),
            @ApiResponse(
                    responseCode = "204",
                    description = "Sin datos en caché - Ejecutar primero GET /api/v1/productos para poblar el catálogo",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProductoListResponse.class))
            )
    })
    @PostMapping(value = "/consultar",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoListResponse> consultarDesdeCachePorFiltros(
            @RequestBody(required = false) ProductoConsultaRequest request) {
        log.info("Recibida petición HTTP POST /api/v1/productos/consultar");
        ProductoListResponse response = productoService.consultarDesdeCacheOBaseDeDatos(request);
        HttpStatus status = (response.getCodigoRespuesta() == CodigoRespuestaEnum.SIN_DATOS_EN_CACHE
                || (response.getCodigo() != null && (response.getCodigo() == 204 || response.getCodigo() == 1)))
                ? HttpStatus.NO_CONTENT
                : HttpStatus.OK;
        return new ResponseEntity<>(response, status);
    }
}
