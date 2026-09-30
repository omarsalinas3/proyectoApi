package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.service.CuentaService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CuentaController")
class CuentaControllerTest {

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    private CuentaResponse cuentaResponse;
    private SaldoResponse saldoResponse;

    @BeforeEach
    void setUp() {
        cuentaResponse = CuentaResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Operación realizada con éxito")
                .titular("Carlos Hernández López")
                .curpTitular("HELC950815HDFRPR03")
                .cuenta(CuentaDTO.builder().numeroCuenta("1234567890123456").estatus("ACTIVA").build())
                .build();

        saldoResponse = SaldoResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Saldo consultado")
                .saldo(SaldoDTO.builder().saldoDisponible(BigDecimal.ZERO).moneda("MXN").build())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/cuentas/{numeroCuenta} debe retornar detalle de la cuenta")
    void testConsultarPorNumeroCuenta() {
        when(cuentaService.consultarPorNumeroCuenta("1234567890123456")).thenReturn(cuentaResponse);

        ResponseEntity<CuentaResponse> response = cuentaController.consultarPorNumeroCuenta("1234567890123456");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("Carlos Hernández López", response.getBody().getTitular());
    }

    @Test
    @DisplayName("GET /api/v1/cuentas/activas debe retornar listado de cuentas activas")
    void testConsultarCuentasActivas() {
        when(cuentaService.consultarCuentasActivas()).thenReturn(List.of(cuentaResponse));

        ResponseEntity<List<CuentaResponse>> response = cuentaController.consultarCuentasActivas();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("GET /api/v1/cuentas/{numeroCuenta}/saldo debe retornar saldo de la cuenta")
    void testConsultarSaldo() {
        when(cuentaService.consultarSaldoPorNumeroCuenta("1234567890123456")).thenReturn(saldoResponse);

        ResponseEntity<SaldoResponse> response = cuentaController.consultarSaldoPorNumeroCuenta("1234567890123456");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
    }
}
