package com.proyecto.servicios.service;

import com.proyecto.servicios.client.ProductosClient;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.ExternalServiceTimeoutException;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la clase de servicio {@link ProductoServiceImpl}.
 *
 * <p>Valida los diferentes escenarios de integración según la especificación
 * técnica de PuntoRed / GestoPago para el endpoint {@code getProductList.do}.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ProductoServiceImpl")
class ProductoServiceImplTest {

    @Mock
    private ProductosClient productosClient;

    @InjectMocks
    private ProductoServiceImpl productoService;

    private ProductoListResponse mockResponse;

    @BeforeEach
    void setUp() {
        ProductoItemDTO item1 = ProductoItemDTO.builder()
                .idProducto(185L)
                .idServicio(56)
                .idCatTipoServicio(15)
                .tipoFront(2)
                .producto("Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)")
                .servicio("AGUAKAN (Cancun)")
                .precio(new BigDecimal("10.0"))
                .tipoReferencia("c")
                .hasDigitoVerificador(false)
                .showAyuda(false)
                .legend("Para cualquier duda o aclaracion con tu pago...")
                .build();

        ProductoItemDTO item2 = ProductoItemDTO.builder()
                .idProducto(200L)
                .idServicio(71)
                .idCatTipoServicio(10)
                .tipoFront(1)
                .producto("Amazon $100")
                .servicio("Amazon")
                .precio(new BigDecimal("100.0"))
                .tipoReferencia("a")
                .hasDigitoVerificador(false)
                .showAyuda(false)
                .legend("Para usar tu tarjeta ingresa a www.amazon.com.mx/gc/redeem/...")
                .build();

        mockResponse = ProductoListResponse.builder()
                .codigo(200)
                .mensaje("Operacion realizada con exito")
                .totalRegistros(2L)
                .fechaConsulta(LocalDateTime.now())
                .productos(List.of(item1, item2))
                .build();
    }

    @Test
    @DisplayName("Debe consultar y retornar la lista de productos de PuntoRed exitosamente")
    void consultarListaProductos_exitoso() {
        // Arrange
        when(productosClient.getProductList()).thenReturn(mockResponse);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado, "La respuesta no debe ser nula");
        assertEquals(200, resultado.getCodigo());
        assertEquals("Operacion realizada con exito", resultado.getMensaje());
        assertEquals(2L, resultado.getTotalRegistros());
        assertNotNull(resultado.getProductos());
        assertEquals(2, resultado.getProductos().size());

        ProductoItemDTO prod1 = resultado.getProductos().get(0);
        assertEquals(185L, prod1.getIdProducto());
        assertEquals(56, prod1.getIdServicio());
        assertEquals("AGUAKAN (Cancun)", prod1.getServicio());
        assertEquals(new BigDecimal("10.0"), prod1.getPrecio());
        assertEquals(2, prod1.getTipoFront());
        assertFalse(prod1.getHasDigitoVerificador());

        verify(productosClient, times(1)).getProductList();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceAuthException cuando falla la autenticación (401/403)")
    void consultarListaProductos_errorAutenticacion() {
        // Arrange
        when(productosClient.getProductList())
                .thenThrow(new ExternalServiceAuthException("Your token is expired. Is just valid for 24 hours", 403));

        // Act & Assert
        ExternalServiceAuthException excepcion = assertThrows(
                ExternalServiceAuthException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceAuthException"
        );

        assertEquals(403, excepcion.getStatusCode());
        assertTrue(excepcion.getMessage().contains("token is expired"));
        verify(productosClient, times(1)).getProductList();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceTimeoutException cuando se agota el tiempo de espera (62s)")
    void consultarListaProductos_timeout() {
        // Arrange
        when(productosClient.getProductList())
                .thenThrow(new ExternalServiceTimeoutException("Tiempo de espera alcanzado"));

        // Act & Assert
        ExternalServiceTimeoutException excepcion = assertThrows(
                ExternalServiceTimeoutException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceTimeoutException"
        );

        assertTrue(excepcion.getMessage().contains("Tiempo de espera alcanzado"));
        verify(productosClient, times(1)).getProductList();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceException cuando el servicio remoto responde con error 500")
    void consultarListaProductos_errorServicioExterno() {
        // Arrange
        when(productosClient.getProductList())
                .thenThrow(new ExternalServiceException("Error interno en servidor de PuntoRed", 500));

        // Act & Assert
        ExternalServiceException excepcion = assertThrows(
                ExternalServiceException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceException"
        );

        assertEquals(500, excepcion.getStatusCode());
        verify(productosClient, times(1)).getProductList();
    }

    @Test
    @DisplayName("Debe manejar defensivamente una respuesta nula del cliente Feign")
    void consultarListaProductos_respuestaNula() {
        // Arrange
        when(productosClient.getProductList()).thenReturn(null);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado, "El servicio debe retornar un DTO por defecto ante respuesta nula");
        assertEquals(204, resultado.getCodigo());
        assertEquals("No se obtuvo contenido del servicio externo", resultado.getMensaje());
        assertNotNull(resultado.getFechaConsulta());
        verify(productosClient, times(1)).getProductList();
    }
}
