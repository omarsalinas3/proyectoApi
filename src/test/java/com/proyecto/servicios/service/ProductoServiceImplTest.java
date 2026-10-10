package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la clase de servicio {@link ProductoServiceImpl}.
 *
 * <p>Valida los diferentes escenarios de integración según la especificación
 * técnica de PuntoRed / GestoPago para el endpoint {@code getProductList.do},
 * incluyendo el parseo de XML nativo y JSON.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ProductoServiceImpl")
class ProductoServiceImplTest {

    @Mock
    private ProductosClient productosClient;

    private ObjectMapper objectMapper;

    private ProductoServiceImpl productoService;

    private String xmlEjemploPuntoRed;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        productoService = new ProductoServiceImpl(productosClient, objectMapper);

        xmlEjemploPuntoRed = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>01</CODIGO>\n" +
                "        <TEXTO>Operacion realizada con exito</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "    <PRODUCTOS>\n" +
                "        <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun (Mun. de Benito Juarez y de Isla Mujeres)' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>\n" +
                "            <legend>\n" +
                "                <![CDATA[Para cualquier duda o aclaracion con tu pago, comunicate al servicio de Atencion a clientes de AGUAKAN al telefono 073.]]>\n" +
                "            </legend>\n" +
                "        </producto>\n" +
                "        <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200' idCatTipoServicio='10' tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>\n" +
                "            <legend>\n" +
                "                <![CDATA[Para usar tu tarjeta ingresa a www.amazon.com.mx/gc/redeem/ e ingresa el codigo de tu tarjeta.]]>\n" +
                "            </legend>\n" +
                "        </producto>\n" +
                "    </PRODUCTOS>\n" +
                "</RESPONSE>";
    }

    @Test
    @DisplayName("Debe parsear y retornar la lista de productos desde la respuesta XML nativa de PuntoRed")
    void consultarListaProductos_exitosoXml() {
        // Arrange
        when(productosClient.getProductListRaw()).thenReturn(xmlEjemploPuntoRed);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado, "La respuesta no debe ser nula");
        assertEquals(1, resultado.getCodigo());
        assertEquals("Operacion realizada con exito", resultado.getMensaje());
        assertEquals(2L, resultado.getTotalRegistros());
        assertNotNull(resultado.getProductos());
        assertEquals(2, resultado.getProductos().size());

        ProductoItemDTO prod1 = resultado.getProductos().get(0);
        assertEquals(185L, prod1.getIdProducto());
        assertEquals(56, prod1.getIdServicio());
        assertEquals(15, prod1.getIdCatTipoServicio());
        assertEquals(2, prod1.getTipoFront());
        assertEquals("AGUAKAN (Cancun)", prod1.getServicio());
        assertEquals(new BigDecimal("10.0"), prod1.getPrecio());
        assertFalse(prod1.getHasDigitoVerificador());
        assertNotNull(prod1.getLegend());
        assertTrue(prod1.getLegend().contains("AGUAKAN"));

        verify(productosClient, times(1)).getProductListRaw();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceAuthException cuando falla la autenticación (401/403)")
    void consultarListaProductos_errorAutenticacion() {
        // Arrange
        when(productosClient.getProductListRaw())
                .thenThrow(new ExternalServiceAuthException("Your token is expired. Is just valid for 24 hours", 403));

        // Act & Assert
        ExternalServiceAuthException excepcion = assertThrows(
                ExternalServiceAuthException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceAuthException"
        );

        assertEquals(403, excepcion.getStatusCode());
        assertTrue(excepcion.getMessage().contains("token is expired"));
        verify(productosClient, times(1)).getProductListRaw();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceTimeoutException cuando se agota el tiempo de espera (62s)")
    void consultarListaProductos_timeout() {
        // Arrange
        when(productosClient.getProductListRaw())
                .thenThrow(new ExternalServiceTimeoutException("Tiempo de espera alcanzado"));

        // Act & Assert
        ExternalServiceTimeoutException excepcion = assertThrows(
                ExternalServiceTimeoutException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceTimeoutException"
        );

        assertTrue(excepcion.getMessage().contains("Tiempo de espera alcanzado"));
        verify(productosClient, times(1)).getProductListRaw();
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceException cuando el servicio remoto responde con error 500")
    void consultarListaProductos_errorServicioExterno() {
        // Arrange
        when(productosClient.getProductListRaw())
                .thenThrow(new ExternalServiceException("Error interno en servidor de PuntoRed", 500));

        // Act & Assert
        ExternalServiceException excepcion = assertThrows(
                ExternalServiceException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceException"
        );

        assertEquals(500, excepcion.getStatusCode());
        verify(productosClient, times(1)).getProductListRaw();
    }

    @Test
    @DisplayName("Debe manejar defensivamente una respuesta nula o vacía del cliente Feign")
    void consultarListaProductos_respuestaVacia() {
        // Arrange
        when(productosClient.getProductListRaw()).thenReturn("");

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado, "El servicio debe retornar un DTO por defecto ante respuesta vacía");
        assertEquals(204, resultado.getCodigo());
        assertEquals("No se obtuvo contenido del servicio externo", resultado.getMensaje());
        assertNotNull(resultado.getFechaConsulta());
        verify(productosClient, times(1)).getProductListRaw();
    }
}
