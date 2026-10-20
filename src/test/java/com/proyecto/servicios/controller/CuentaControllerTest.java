package com.proyecto.servicios.controller;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.CuentaSaldoRequest;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.model.common.PaginaResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.service.CuentaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CuentaController")
class CuentaControllerTest {

    @Mock
    private CuentaService cuentaService;

    @Mock
    private ControlAcceso controlAcceso;

    @InjectMocks
    private CuentaController cuentaController;

    @Test
    @DisplayName("POST /api/v1/cuentas/buscar debe limitar los filtros al usuario y devolver la página")
    void testBuscarCuentas() {
        CuentaBusquedaRequest filtros = CuentaBusquedaRequest.builder().estatus("ACTIVA").build();
        CuentaResponse cuenta = CuentaResponse.builder()
                .titular("Carlos Hernández López")
                .cuenta(CuentaDTO.builder().numeroCuenta("1234567890123456").estatus("ACTIVA").build())
                .build();
        when(controlAcceso.limitarBusquedaCuentas(filtros)).thenReturn(filtros);
        when(cuentaService.buscar(filtros)).thenReturn(PaginaResponse.<CuentaResponse>builder()
                .contenido(List.of(cuenta)).pagina(0).tamanio(20).totalElementos(1).totalPaginas(1).build());

        ResponseEntity<RespuestaApi<PaginaResponse<CuentaResponse>>> response = cuentaController.buscarCuentas(filtros);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("1234567890123456", response.getBody().getDatos().getContenido().get(0).getCuenta().getNumeroCuenta());
    }

    @Test
    @DisplayName("POST /api/v1/cuentas/saldo debe devolver el saldo de la cuenta propia")
    void testConsultarSaldo() {
        SaldoResponse saldo = SaldoResponse.builder()
                .saldo(SaldoDTO.builder().saldoDisponible(BigDecimal.ZERO).moneda("MXN").build())
                .build();
        when(cuentaService.consultarSaldoPorNumeroCuenta("1234567890123456")).thenReturn(saldo);

        ResponseEntity<RespuestaApi<SaldoResponse>> response =
                cuentaController.consultarSaldo(CuentaSaldoRequest.builder().numeroCuenta("1234567890123456").build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("MXN", response.getBody().getDatos().getSaldo().getMoneda());
        verify(controlAcceso).validarRecursoPropio(any(), eq("la cuenta solicitada"));
    }

    @Test
    @DisplayName("POST /api/v1/cuentas/saldo de una cuenta ajena no debe consultar el saldo")
    void testConsultarSaldoAjeno() {
        doThrow(new AccesoDenegadoException("sin permiso")).when(controlAcceso).validarRecursoPropio(any(), eq("la cuenta solicitada"));

        assertThrows(AccesoDenegadoException.class, () ->
                cuentaController.consultarSaldo(CuentaSaldoRequest.builder().numeroCuenta("9999999999999999").build()));
        verifyNoInteractions(cuentaService);
    }
}
