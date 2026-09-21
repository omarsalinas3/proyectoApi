package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.request.ProductoConsultaRequest;
import com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link ProductoController}.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ProductoController")
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ProductoController productoController;

    private ProductoListResponse respuestaExitosa;
    private ProductoAgrupadoResponse respuestaAgrupada;

    @BeforeEach
    void setUp() {
        respuestaExitosa = ProductoListResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Operación realizada con éxito")
                .origenDatos("SERVICIO_EXTERNO")
                .totalRegistros(1L)
                .fechaConsulta(LocalDateTime.now())
                .productos(List.of(
                        ProductoItemDTO.builder().idProducto(101L).tipoFront(1).producto("Recarga").build()
                ))
                .build();

        Map<Integer, List<ProductoItemDTO>> grupos = new HashMap<>();
        grupos.put(1, List.of(ProductoItemDTO.builder().idProducto(101L).tipoFront(1).build()));
        grupos.put(2, Collections.emptyList());
        grupos.put(3, Collections.emptyList());

        respuestaAgrupada = ProductoAgrupadoResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Productos agrupados exitosamente por tipoFront")
                .origenDatos("SERVICIO_EXTERNO")
                .totalRegistros(1L)
                .fechaConsulta(LocalDateTime.now())
                .grupos(grupos)
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/productos debe retornar HTTP 200 y ProductoListResponse con código 0")
    void obtenerListaProductos_exitoso() {
        // Arrange
        when(productoService.consultarListaProductos()).thenReturn(respuestaExitosa);

        // Act
        ResponseEntity<ProductoListResponse> response = productoController.obtenerListaProductos();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.EXITO, response.getBody().getCodigoRespuesta());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals(1, response.getBody().getProductos().size());
        verify(productoService, times(1)).consultarListaProductos();
    }

    @Test
    @DisplayName("GET /api/v1/productos/agrupados-por-tipo-front debe retornar HTTP 200 y grupos 1, 2 y 3")
    void obtenerProductosAgrupadosPorTipoFront_exitoso() {
        // Arrange
        when(productoService.obtenerProductosAgrupadosPorTipoFront()).thenReturn(respuestaAgrupada);

        // Act
        ResponseEntity<ProductoAgrupadoResponse> response = productoController.obtenerProductosAgrupadosPorTipoFront();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.EXITO, response.getBody().getCodigoRespuesta());
        assertEquals(0, response.getBody().getCodigo());
        assertTrue(response.getBody().getGrupos().containsKey(1));
        assertTrue(response.getBody().getGrupos().containsKey(2));
        assertTrue(response.getBody().getGrupos().containsKey(3));
        verify(productoService, times(1)).obtenerProductosAgrupadosPorTipoFront();
    }

    @Test
    @DisplayName("POST /api/v1/productos/consultar debe retornar HTTP 200 cuando hay datos en caché")
    void consultarDesdeCachePorFiltros_conDatos() {
        // Arrange
        when(productoService.consultarDesdeCacheOBaseDeDatos(any(ProductoConsultaRequest.class)))
                .thenReturn(respuestaExitosa);

        ProductoConsultaRequest request = ProductoConsultaRequest.builder().tipoFront(1).build();

        // Act
        ResponseEntity<ProductoListResponse> response = productoController.consultarDesdeCachePorFiltros(request);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCodigo());
        verify(productoService, times(1)).consultarDesdeCacheOBaseDeDatos(request);
    }

    @Test
    @DisplayName("POST /api/v1/productos/consultar debe retornar HTTP 204 No Content cuando no hay datos en caché")
    void consultarDesdeCachePorFiltros_sinDatos() {
        // Arrange
        ProductoListResponse respuestaSinDatos = ProductoListResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE)
                .codigo(1)
                .mensaje(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE.getDescripcion())
                .totalRegistros(0L)
                .productos(Collections.emptyList())
                .build();

        when(productoService.consultarDesdeCacheOBaseDeDatos(any()))
                .thenReturn(respuestaSinDatos);

        // Act
        ResponseEntity<ProductoListResponse> response = productoController.consultarDesdeCachePorFiltros(null);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE, response.getBody().getCodigoRespuesta());
        assertEquals(1, response.getBody().getCodigo());
    }
}
