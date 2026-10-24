package com.proyecto.servicios.handler.transaccion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.ProductoNoDisponibleException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.repositorys.producto.ProductoCatalogoRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas Unitarias - ArmadoPayloadHandler (nodo «Sen Tx»)")
class ArmadoPayloadHandlerTest {

    /** 2026-10-23 18:30:45 UTC = 12:30:45 en Ciudad de México. */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-23T18:30:45Z"), ZoneOffset.UTC);

    private final ProductoCatalogoRepository productos = mock(ProductoCatalogoRepository.class);
    private final TransaccionRepository transacciones = mock(TransaccionRepository.class);
    private ArmadoPayloadHandler handler;

    @BeforeEach
    void setUp() {
        when(transacciones.existsByUpc(anyString())).thenReturn(false);
        handler = new ArmadoPayloadHandler(productos, transacciones, new ObjectMapper(), RELOJ, "083", "America/Mexico_City");
    }

    private static ProductoCatalogoEntity producto(long id, int servicio, int tipoFront, Integer tipoServicio, String precio) {
        return ProductoCatalogoEntity.builder().idProducto(id).idServicio(servicio).tipoFront(tipoFront)
                .idCatTipoServicio(tipoServicio).producto("Producto " + id).precio(precio == null ? null : new BigDecimal(precio))
                .build();
    }

    private static ContextoTransaccion contexto(TransaccionRequest request) {
        return new ContextoTransaccion(request, new UsuarioAutenticado(1L, "ana", RolEnum.CLIENTE));
    }

    private static TransaccionRequest.TransaccionRequestBuilder recarga() {
        return TransaccionRequest.builder().numCuenta("4152000000000001").idServicio(133).idProducto(582L)
                .monto(new BigDecimal("10")).telefono("5555555551").codigo2fa("123456");
    }

    @Test
    @DisplayName("Recarga (tipoFront 1): envía el teléfono capturado, sin referencia ni montoPago")
    void payloadDeRecarga() {
        when(productos.findById(582L)).thenReturn(Optional.of(producto(582, 133, 1, null, "10.00")));
        ContextoTransaccion ctx = contexto(recarga().referencia("ignorada").build());

        handler.manejar(ctx);

        SendTxPayload p = ctx.getPayload();
        assertEquals("5555555551", p.getTelefono());
        assertNull(p.getReferencia());
        assertNull(p.getMontoPago());
        assertEquals("20261023123045", p.getHoraLocal());
        assertEquals("083", p.getUnidad());
        assertTrue(p.getUpc().matches("TX[0-9A-Z]{8}"), p.getUpc());
        assertEquals("{\"idProducto\":582,\"idServicio\":133,\"telefono\":\"5555555551\",\"horaLocal\":\"20261023123045\","
                + "\"upc\":\"" + p.getUpc() + "\",\"unidad\":\"083\"}", ctx.getPayloadJson());
    }

    @Test
    @DisplayName("Pago de servicio (tipoFront 2, ej. luz o agua): teléfono fijo, referencia y montoPago del recibo aunque el catálogo indique otro precio")
    void pagoDeServicio() {
        when(productos.findById(205L)).thenReturn(Optional.of(producto(205, 76, 2, null, "10.00")));
        ContextoTransaccion ctx = contexto(TransaccionRequest.builder().numCuenta("4152000000000001").idServicio(76)
                .idProducto(205L).monto(new BigDecimal("601.99")).referencia("25720381").codigo2fa("123456").build());

        handler.manejar(ctx);

        SendTxPayload p = ctx.getPayload();
        assertEquals("1111111111", p.getTelefono());
        assertEquals("25720381", p.getReferencia());
        assertEquals(new BigDecimal("601.99"), p.getMontoPago());
        assertTrue(ctx.getPayloadJson().contains("\"referencia\":\"25720381\",\"montoPago\":601.99"), ctx.getPayloadJson());
    }

    @Test
    @DisplayName("idCatTipoServicio 10 u 11: el teléfono va fijo en #1111111111")
    void tipoServicioEspecial() {
        when(productos.findById(900L)).thenReturn(Optional.of(producto(900, 300, 1, 10, "200.00")));
        ContextoTransaccion ctx = contexto(TransaccionRequest.builder().numCuenta("4152000000000001").idServicio(300)
                .idProducto(900L).monto(new BigDecimal("200")).referencia("X1").codigo2fa("123456").build());

        handler.manejar(ctx);

        assertEquals("#1111111111", ctx.getPayload().getTelefono());
    }

    @Test
    @DisplayName("Un pago de servicio sin referencia o una recarga sin teléfono se rechazan con mensaje específico")
    void datoSegunTipoDeProducto() {
        when(productos.findById(205L)).thenReturn(Optional.of(producto(205, 76, 2, null, "10.00")));
        when(productos.findById(582L)).thenReturn(Optional.of(producto(582, 133, 1, null, "10.00")));

        ValidacionNegocioException servicio = assertThrows(ValidacionNegocioException.class, () -> handler.manejar(contexto(
                TransaccionRequest.builder().numCuenta("4152000000000001").idServicio(76).idProducto(205L)
                        .monto(BigDecimal.TEN).telefono("5555555551").codigo2fa("123456").build())));
        assertTrue(servicio.getMessage().contains("referencia"));

        ValidacionNegocioException sinTelefono = assertThrows(ValidacionNegocioException.class,
                () -> handler.manejar(contexto(recarga().telefono(null).referencia("ABC").build())));
        assertTrue(sinTelefono.getMessage().contains("número telefónico"));
    }

    @Test
    @DisplayName("Producto inexistente, de otro servicio o con monto distinto a su precio fijo")
    void productoNoDisponible() {
        when(productos.findById(582L)).thenReturn(Optional.of(producto(582, 133, 1, null, "10.00")));
        when(productos.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductoNoDisponibleException.class, () -> handler.manejar(contexto(recarga().idProducto(999L).build())));
        assertThrows(ProductoNoDisponibleException.class, () -> handler.manejar(contexto(recarga().idServicio(1).build())));
        ProductoNoDisponibleException precio = assertThrows(ProductoNoDisponibleException.class,
                () -> handler.manejar(contexto(recarga().monto(new BigDecimal("20")).build())));
        assertTrue(precio.getMessage().contains("$10.00"));
    }

    @Test
    @DisplayName("La unidad configurada debe tener entre 3 y 25 caracteres")
    void unidadInvalida() {
        assertThrows(IllegalStateException.class,
                () -> new ArmadoPayloadHandler(productos, transacciones, new ObjectMapper(), RELOJ, "08", "UTC"));
    }
}
