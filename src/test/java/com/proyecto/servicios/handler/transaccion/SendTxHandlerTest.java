package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.DosFactoresInvalidoException;
import com.proyecto.servicios.exception.SaldoInsuficienteException;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - SendTxHandler (orden del flujo)")
class SendTxHandlerTest {

    @Mock private ClienteActivoHandler clienteActivoHandler;
    @Mock private SaldoHandler saldoHandler;
    @Mock private DosFactoresHandler dosFactoresHandler;
    @Mock private ArmadoPayloadHandler armadoPayloadHandler;
    @Mock private CifradoPeticionHandler cifradoPeticionHandler;
    @Mock private TokenHandler tokenHandler;
    @Mock private RegistroTransaccion registroTransaccion;
    @Mock private EnvioTransaccionHandler envioTransaccionHandler;
    @Mock private ResultadoTransaccionHandler resultadoTransaccionHandler;

    @InjectMocks
    private SendTxHandler sendTxHandler;

    private final UsuarioAutenticado cliente = new UsuarioAutenticado(1L, "ana", RolEnum.CLIENTE);
    private final TransaccionRequest request = TransaccionRequest.builder().numCuenta("4152000000000001")
            .idServicio(133).idProducto(582L).monto(BigDecimal.TEN).telefono("5555555551").codigo2fa("123456").build();

    @Test
    @DisplayName("Ejecuta los nodos en el orden del diagrama y devuelve la respuesta del registro")
    void ordenDelFlujo() {
        doAnswer(i -> {
            i.<ContextoTransaccion>getArgument(0).setPayload(SendTxPayload.builder().upc("TXABC").build());
            return null;
        }).when(armadoPayloadHandler).manejar(any());
        TransaccionResponse exitosa = TransaccionResponse.builder().upc("TXABC").estatus(EstatusTransaccionEnum.EXITOSA).build();
        doAnswer(i -> {
            i.<ContextoTransaccion>getArgument(0).setRespuesta(exitosa);
            return null;
        }).when(resultadoTransaccionHandler).manejar(any());

        TransaccionResponse respuesta = sendTxHandler.ejecutar(request, cliente);

        assertSame(exitosa, respuesta);
        InOrder orden = inOrder(clienteActivoHandler, saldoHandler, dosFactoresHandler, armadoPayloadHandler,
                cifradoPeticionHandler, tokenHandler, registroTransaccion, envioTransaccionHandler, resultadoTransaccionHandler);
        orden.verify(clienteActivoHandler).manejar(any());
        orden.verify(saldoHandler).manejar(any());
        orden.verify(dosFactoresHandler).manejar(any());
        orden.verify(armadoPayloadHandler).manejar(any());
        orden.verify(cifradoPeticionHandler).manejar(any());
        orden.verify(tokenHandler).manejar(any());
        orden.verify(registroTransaccion).reservar(any());
        orden.verify(envioTransaccionHandler).manejar(any());
        orden.verify(resultadoTransaccionHandler).manejar(any());
    }

    @Test
    @DisplayName("Si el cliente no está activo no se ejecuta ningún paso posterior")
    void clienteInactivoDetieneElFlujo() {
        doThrow(new ClienteInactivoException("baja")).when(clienteActivoHandler).manejar(any());

        assertThrows(ClienteInactivoException.class, () -> sendTxHandler.ejecutar(request, cliente));
        verifyNoInteractions(saldoHandler, dosFactoresHandler, armadoPayloadHandler, cifradoPeticionHandler,
                tokenHandler, registroTransaccion, envioTransaccionHandler, resultadoTransaccionHandler);
    }

    @Test
    @DisplayName("Sin saldo no se valida el 2FA ni se cifra o envía nada")
    void saldoInsuficienteDetieneElFlujo() {
        doThrow(new SaldoInsuficienteException("sin saldo")).when(saldoHandler).manejar(any());

        assertThrows(SaldoInsuficienteException.class, () -> sendTxHandler.ejecutar(request, cliente));
        verifyNoInteractions(dosFactoresHandler, armadoPayloadHandler, cifradoPeticionHandler, tokenHandler,
                registroTransaccion, envioTransaccionHandler);
    }

    @Test
    @DisplayName("Con 2FA inválido no se cifra, no se pide token ni se envía la transacción")
    void dosFactoresInvalidoDetieneElFlujo() {
        doThrow(new DosFactoresInvalidoException("código")).when(dosFactoresHandler).manejar(any());

        assertThrows(DosFactoresInvalidoException.class, () -> sendTxHandler.ejecutar(request, cliente));
        verifyNoInteractions(armadoPayloadHandler, cifradoPeticionHandler, tokenHandler, registroTransaccion,
                envioTransaccionHandler, resultadoTransaccionHandler);
    }
}
