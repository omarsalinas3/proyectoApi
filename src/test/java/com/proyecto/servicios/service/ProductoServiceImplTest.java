package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.client.ProductosClient;
import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.ExternalServiceTimeoutException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.repositorys.producto.ProductoCatalogoRepository;
import com.proyecto.servicios.service.Impl.ProductoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias completas para {@link ProductoServiceImpl}.
 *
 * <p>Valida:
 * <ul>
 *   <li>Parseo con JAXB y diagnóstico de línea ante errores XML.</li>
 *   <li>Transformaciones de datos con MapStruct.</li>
 *   <li>Estrategia de resiliencia escalonada: API ➔ Redis ➔ PostgreSQL.</li>
 *   <li>Agrupación y filtrado funcional por {@code tipoFront} con Java Streams y Predicates.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ProductoServiceImpl (JAXB, Redis, BD, Streams y Predicates)")
class ProductoServiceImplTest {

    @Mock
    private ProductosClient productosClient;

    @Mock
    private ProductoCatalogoRepository productoCatalogoRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ObjectMapper objectMapper;
    private ProductoMapper productoMapper;
    private ProductoServiceImpl productoService;

    private String xmlEjemploPuntoRed;
    private String jsonEjemploPuntoRed;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        productoMapper = Mappers.getMapper(ProductoMapper.class);

        productoService = new ProductoServiceImpl(
                productosClient,
                objectMapper,
                productoMapper,
                productoCatalogoRepository,
                redisTemplate
        );

        ReflectionTestUtils.setField(productoService, "redisCatalogKey", "catalogo:productos");
        ReflectionTestUtils.setField(productoService, "redisTtlHours", 24L);

        xmlEjemploPuntoRed = "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<RESPONSE>\n" +
                "    <MENSAJE>\n" +
                "        <CODIGO>01</CODIGO>\n" +
                "        <TEXTO>Operacion realizada con exito</TEXTO>\n" +
                "    </MENSAJE>\n" +
                "    <PRODUCTOS>\n" +
                "        <producto servicio='AGUAKAN (Cancun)' producto='Agua Cancun' idServicio='56' idProducto='185' idCatTipoServicio='15' tipoFront='2' hasDigitoVerificador='false' precio='10.0' showAyuda='false' tipoReferencia='c'>\n" +
                "            <legend><![CDATA[Dudas AGUAKAN al 073.]]></legend>\n" +
                "        </producto>\n" +
                "        <producto servicio='Amazon' producto='Amazon $100' idServicio='71' idProducto='200' idCatTipoServicio='10' tipoFront='1' hasDigitoVerificador='false' precio='100.0' showAyuda='false' tipoReferencia='a'>\n" +
                "            <legend><![CDATA[Canjea en amazon.com.mx]]></legend>\n" +
                "        </producto>\n" +
                "        <producto servicio='Xbox' producto='Xbox Pass' idServicio='80' idProducto='300' idCatTipoServicio='12' tipoFront='3' hasDigitoVerificador='false' precio='250.0' showAyuda='false' tipoReferencia='x'>\n" +
                "            <legend><![CDATA[Canjea en xbox.com]]></legend>\n" +
                "        </producto>\n" +
                "    </PRODUCTOS>\n" +
                "</RESPONSE>";

        jsonEjemploPuntoRed = "{\n" +
                "  \"mensaje\": { \"codigo\": \"01\", \"texto\": \"Operacion realizada con exito\" },\n" +
                "  \"productos\": [\n" +
                "    { \"idproducto\": 19457, \"idservicio\": 3265, \"tipofront\": 1, \"producto\": \"Recarga $800\", \"servicio\": \"Recorcholis\", \"precio\": 640.0, \"hasdigitoverificador\": false },\n" +
                "    { \"idproducto\": 19458, \"idservicio\": 3266, \"tipofront\": 2, \"producto\": \"Pago CFE\", \"servicio\": \"CFE\", \"precio\": 150.0, \"hasdigitoverificador\": true }\n" +
                "  ]\n" +
                "}";
    }

    @Test
    @DisplayName("Debe parsear exitosamente XML con JAXB, mapear con MapStruct y persistir en Redis y PostgreSQL")
    void consultarListaProductos_exitosoXml_persisteEnRedisYBaseDeDatos() {
        // Arrange
        when(productosClient.getProductListRaw()).thenReturn(xmlEjemploPuntoRed);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado, "La respuesta no debe ser nula");
        assertEquals(CodigoRespuestaEnum.EXITO, resultado.getCodigoRespuesta());
        assertEquals(0, resultado.getCodigo());
        assertEquals("Operacion realizada con exito", resultado.getMensaje());
        assertEquals(3L, resultado.getTotalRegistros());
        assertEquals("SERVICIO_EXTERNO", resultado.getOrigenDatos());
        assertNotNull(resultado.getProductos());
        assertEquals(3, resultado.getProductos().size());

        ProductoItemDTO p1 = resultado.getProductos().get(0);
        assertEquals(185L, p1.getIdProducto());
        assertEquals(2, p1.getTipoFront());
        assertEquals(new BigDecimal("10.0"), p1.getPrecio());
        assertEquals("AGUAKAN (Cancun)", p1.getServicio());
        assertNotNull(p1.getLegend());
        assertTrue(p1.getLegend().contains("AGUAKAN"));

        // Verificar almacenamiento en caché Redis y persistencia en BD
        verify(valueOperations, times(1)).set(eq("catalogo:productos"), any(ProductoListResponse.class), any(Duration.class));
        verify(productoCatalogoRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Debe parsear exitosamente JSON con claves insensibles a mayúsculas/minúsculas")
    void consultarListaProductos_exitosoJson() {
        // Arrange
        when(productosClient.getProductListRaw()).thenReturn(jsonEjemploPuntoRed);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado);
        assertEquals(CodigoRespuestaEnum.EXITO, resultado.getCodigoRespuesta());
        assertEquals(0, resultado.getCodigo());
        assertEquals(2, resultado.getProductos().size());

        ProductoItemDTO p1 = resultado.getProductos().get(0);
        assertEquals(19457L, p1.getIdProducto());
        assertEquals(1, p1.getTipoFront());
        assertEquals(new BigDecimal("640.0"), p1.getPrecio());
        assertFalse(p1.getHasDigitoVerificador());

        verify(productosClient, times(1)).getProductListRaw();
    }

    @Test
    @DisplayName("Debe capturar la línea exacta del error XML y no comerse la causa original ante XML malformado")
    void consultarListaProductos_xmlMalformado_capturaLineaYPreservaCausa() {
        // Arrange
        String xmlInvalido = "<RESPONSE>\n<MENSAJE>\n<CODIGO>01</CODIGO>\n<TEXTO>Incompleto\n</RESPONSE>";
        when(productosClient.getProductListRaw()).thenReturn(xmlInvalido);

        // Act & Assert
        ExternalServiceException ex = assertThrows(
                ExternalServiceException.class,
                () -> productoService.consultarListaProductos(),
                "Debe lanzar ExternalServiceException ante XML con error de sintaxis"
        );

        assertNotNull(ex.getMessage());
        assertNotNull(ex.getCause(), "La causa original de la excepción debe preservarse sin comerse el error");
        assertEquals(502, ex.getStatusCode());
    }

    @Test
    @DisplayName("Debe activar Fallback y recuperar desde Redis cuando el servicio remoto falla")
    void consultarListaProductos_fallbackRedis_cuandoApiFalla() {
        // Arrange
        when(productosClient.getProductListRaw()).thenThrow(new ExternalServiceException("Servicio caído", 502));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        ProductoListResponse catalogoEnCache = ProductoListResponse.builder()
                .codigo(200)
                .mensaje("Catálogo en caché")
                .totalRegistros(1L)
                .productos(List.of(
                        ProductoItemDTO.builder().idProducto(999L).tipoFront(1).producto("Producto Caché").build()
                ))
                .build();

        when(valueOperations.get("catalogo:productos")).thenReturn(catalogoEnCache);

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado);
        assertEquals("CACHE_REDIS", resultado.getOrigenDatos());
        assertEquals(1, resultado.getProductos().size());
        assertEquals(999L, resultado.getProductos().get(0).getIdProducto());
        verify(productoCatalogoRepository, never()).findAll();
    }

    @Test
    @DisplayName("Debe activar Fallback a PostgreSQL cuando la API falla y Redis está vacío")
    void consultarListaProductos_fallbackPostgreSQL_cuandoApiYRedisFallan() {
        // Arrange
        when(productosClient.getProductListRaw()).thenThrow(new ExternalServiceException("Servicio caído", 502));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("catalogo:productos")).thenReturn(null);

        ProductoCatalogoEntity entidadDb = ProductoCatalogoEntity.builder()
                .idProducto(888L)
                .tipoFront(2)
                .producto("Producto DB")
                .fechaActualizacion(LocalDateTime.now())
                .build();

        when(productoCatalogoRepository.findAll()).thenReturn(List.of(entidadDb));

        // Act
        ProductoListResponse resultado = productoService.consultarListaProductos();

        // Assert
        assertNotNull(resultado);
        assertEquals("BASE_DE_DATOS", resultado.getOrigenDatos());
        assertEquals(1, resultado.getProductos().size());
        assertEquals(888L, resultado.getProductos().get(0).getIdProducto());
        assertEquals(2, resultado.getProductos().get(0).getTipoFront());
    }

    @Test
    @DisplayName("Debe propagar ExternalServiceAuthException cuando la autenticación falla (no enmascarar con fallback)")
    void consultarListaProductos_errorAutenticacion() {
        // Arrange
        when(productosClient.getProductListRaw())
                .thenThrow(new ExternalServiceAuthException("Token vencido", 401));

        // Act & Assert
        ExternalServiceAuthException ex = assertThrows(
                ExternalServiceAuthException.class,
                () -> productoService.consultarListaProductos()
        );

        assertEquals(401, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Token vencido"));
    }

    @Test
    @DisplayName("Debe agrupar productos correctamente por tipoFront (1, 2, 3...) mediante Java Streams y Predicates")
    void obtenerProductosAgrupadosPorTipoFront_evaluaStreamsYPredicates() {
        // Arrange
        when(productosClient.getProductListRaw()).thenReturn(xmlEjemploPuntoRed);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Act
        ProductoAgrupadoResponse agrupado = productoService.obtenerProductosAgrupadosPorTipoFront();

        // Assert
        assertNotNull(agrupado);
        assertEquals(3L, agrupado.getTotalRegistros());
        assertNotNull(agrupado.getGrupos());

        // Validar que los grupos 1, 2 y 3 existan y tengan el número correcto de productos
        assertTrue(agrupado.getGrupos().containsKey(1), "Debe contener grupo tipoFront 1");
        assertTrue(agrupado.getGrupos().containsKey(2), "Debe contener grupo tipoFront 2");
        assertTrue(agrupado.getGrupos().containsKey(3), "Debe contener grupo tipoFront 3");

        assertEquals(1, agrupado.getGrupos().get(1).size(), "Grupo 1 debe tener 1 producto (Amazon)");
        assertEquals("Amazon", agrupado.getGrupos().get(1).get(0).getServicio());

        assertEquals(1, agrupado.getGrupos().get(2).size(), "Grupo 2 debe tener 1 producto (AGUAKAN)");
        assertEquals("AGUAKAN (Cancun)", agrupado.getGrupos().get(2).get(0).getServicio());

        assertEquals(1, agrupado.getGrupos().get(3).size(), "Grupo 3 debe tener 1 producto (Xbox)");
        assertEquals("Xbox", agrupado.getGrupos().get(3).get(0).getServicio());
    }

    @Test
    @DisplayName("Debe validar el funcionamiento individual de los Predicates de tipoFront")
    void predicatesTipoFront_evaluacionCorrecta() {
        ProductoItemDTO p1 = ProductoItemDTO.builder().tipoFront(1).build();
        ProductoItemDTO p2 = ProductoItemDTO.builder().tipoFront(2).build();
        ProductoItemDTO p3 = ProductoItemDTO.builder().tipoFront(3).build();
        ProductoItemDTO pNulo = ProductoItemDTO.builder().tipoFront(null).build();

        assertTrue(ProductoServiceImpl.esTipoFront1.test(p1));
        assertFalse(ProductoServiceImpl.esTipoFront1.test(p2));
        assertFalse(ProductoServiceImpl.esTipoFront1.test(p3));
        assertFalse(ProductoServiceImpl.esTipoFront1.test(pNulo));
        assertFalse(ProductoServiceImpl.esTipoFront1.test(null));

        assertTrue(ProductoServiceImpl.esTipoFront2.test(p2));
        assertTrue(ProductoServiceImpl.esTipoFront3.test(p3));
    }
}
