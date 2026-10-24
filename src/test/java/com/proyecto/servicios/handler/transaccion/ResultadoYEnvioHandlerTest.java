package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.TransaccionPendienteException;
import com.proyecto.servicios.exception.TransaccionRechazadaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.util.GestoPagoXmlParser;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas Unitarias - Envío y resultado de la transacción")
class ResultadoYEnvioHandlerTest {

    private static final String XML_EXITO = "<RESPONSE><ID_TX>291437188</ID_TX><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>"
            + "<MENSAJE><CODIGO>01</CODIGO><TEXTO>Operacion realizada con exito</TEXTO></MENSAJE></RESPONSE>";

    private final RegistroTransaccion registro = mock(RegistroTransaccion.class);
    private final GestoPagoTransaccionClient cliente = mock(GestoPagoTransaccionClient.class);
    private final TokenHandler tokenHandler = mock(TokenHandler.class);
    private final EnvioTransaccionHandler envio = new EnvioTransaccionHandler(cliente, tokenHandler, new GestoPagoXmlParser(), registro);
    private final ResultadoTransaccionHandler resultado = new ResultadoTransaccionHandler(registro);

    private ContextoTransaccion ctx;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resultado, "esperaConfirmacionSegundos", 62L);
        ctx = new ContextoTransaccion(null, new UsuarioAutenticado(1L, "ana", RolEnum.CLIENTE));
        ctx.setTransaccionId(7L);
        ctx.setPayload(SendTxPayload.builder().upc("TXABC").build());
        ctx.setRandom("1761243045000");
        ctx.setPeticionCifrada("cifrado==");
        ctx.setToken("token-vigente");
        when(registro.registrarExitosa(anyLong(), any(), any()))
                .thenReturn(TransaccionResponse.builder().upc("TXABC").estatus(EstatusTransaccionEnum.EXITOSA).build());
        when(registro.registrarRechazada(anyLong(), any(), any()))
                .thenReturn(TransaccionResponse.builder().upc("TXABC").estatus(EstatusTransaccionEnum.RECHAZADA).build());
        when(registro.registrarPendiente(anyLong(), any(), any()))
                .thenReturn(TransaccionResponse.builder().upc("TXABC").estatus(EstatusTransaccionEnum.PENDIENTE_CONFIRMACION).build());
    }

    private static GestoPagoTxResultado codigo(String codigo, String autorizacion) {
        return GestoPagoTxResultado.builder().codigo(codigo).texto("texto " + codigo).numAutorizacion(autorizacion).build();
    }

    @Test
    @DisplayName("Envía random + signed con el token Bearer e interpreta la respuesta")
    void envioConTokenYFormulario() {
        when(cliente.sendTx(eq("Bearer token-vigente"), anyMap())).thenReturn(XML_EXITO);

        envio.manejar(ctx);

        verify(cliente).sendTx("Bearer token-vigente", Map.of("random", "1761243045000", "signed", "cifrado=="));
        assertTrue(ctx.getResultado().esExitosa());
        assertEquals(XML_EXITO, ctx.getRespuestaXml());
    }

    @Test
    @DisplayName("Token expirado (403): renueva el token y reintenta una sola vez")
    void tokenExpiradoRenuevaYReintenta() {
        when(cliente.sendTx(eq("Bearer token-vigente"), anyMap())).thenThrow(new ExternalServiceAuthException("EXPIRED", 403));
        doAnswer(i -> {
            i.<ContextoTransaccion>getArgument(0).setToken("token-nuevo");
            return null;
        }).when(tokenHandler).renovar(ctx);
        when(cliente.sendTx(eq("Bearer token-nuevo"), anyMap())).thenReturn(XML_EXITO);

        envio.manejar(ctx);

        assertTrue(ctx.getResultado().esExitosa());
        verify(cliente, times(2)).sendTx(anyString(), anyMap());
    }

    @Test
    @DisplayName("Sin respuesta (timeout): el resultado queda incierto y no se libera el monto")
    void timeoutQuedaSinResultado() {
        when(cliente.sendTx(anyString(), anyMap())).thenThrow(new RetryableException(-1, "Read timed out", Request.HttpMethod.POST,
                (Long) null, Request.create(Request.HttpMethod.POST, "/sendTx.do", Map.of(), new byte[0], StandardCharsets.UTF_8, null)));

        envio.manejar(ctx);

        assertNull(ctx.getResultado());
        verify(registro, never()).registrarRechazada(anyLong(), any(), any());
    }

    @Test
    @DisplayName("Un 4xx en sendTx libera el monto; un 4xx en confirmTx no (el resultado original sigue incierto)")
    void error4xx() {
        when(cliente.sendTx(anyString(), anyMap())).thenThrow(new ExternalServiceException("bad request", 400));
        when(cliente.confirmTx(anyString(), anyMap())).thenThrow(new ExternalServiceException("bad request", 400));

        assertThrows(ExternalServiceException.class, () -> envio.manejar(ctx));
        verify(registro).registrarRechazada(7L, null, null);

        clearInvocations(registro);
        assertThrows(ExternalServiceException.class, () -> envio.confirmar(ctx));
        verify(registro, never()).registrarRechazada(anyLong(), any(), any());
    }

    @Test
    @DisplayName("CODIGO 01: registra la transacción como exitosa")
    void resultadoExitoso() {
        ctx.setResultado(codigo("01", "100024988"));

        resultado.manejar(ctx);

        assertEquals(EstatusTransaccionEnum.EXITOSA, ctx.getRespuesta().getEstatus());
    }

    @Test
    @DisplayName("Otro código (ej. 02 o 06 duplicada): rechazada, se libera el monto y se informa el código")
    void resultadoRechazado() {
        ctx.setResultado(codigo("02", "-1"));

        TransaccionRechazadaException ex = assertThrows(TransaccionRechazadaException.class, () -> resultado.manejar(ctx));

        assertEquals("02", ex.getCodigoProveedor());
        verify(registro).registrarRechazada(eq(7L), any(), any());
    }

    @Test
    @DisplayName("CODIGO 82 o sin respuesta: queda pendiente de confirmación")
    void resultadoPendiente() {
        ctx.setResultado(codigo("82", "-1"));
        assertThrows(TransaccionPendienteException.class, () -> resultado.manejar(ctx));

        ctx.setResultado(null);
        TransaccionPendienteException ex = assertThrows(TransaccionPendienteException.class, () -> resultado.manejar(ctx));
        assertTrue(ex.getMessage().contains("62 segundos"));
        verify(registro, never()).registrarRechazada(anyLong(), any(), any());
    }

    @Test
    @DisplayName("confirmTx: 06 con autorización = aplicada; 70 = no aplicada")
    void resultadoConfirmacion() {
        ctx.setResultado(codigo("06", "100024988"));
        resultado.manejarConfirmacion(ctx);
        assertEquals(EstatusTransaccionEnum.EXITOSA, ctx.getRespuesta().getEstatus());

        ctx.setResultado(codigo("70", "0"));
        assertThrows(TransaccionRechazadaException.class, () -> resultado.manejarConfirmacion(ctx));
    }
}
