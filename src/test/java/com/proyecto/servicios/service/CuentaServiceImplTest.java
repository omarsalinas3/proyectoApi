package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.service.Impl.CuentaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CuentaServiceImpl")
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private SaldoRepository saldoRepository;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    private CuentaEntity cuentaEntity;
    private SaldoEntity saldoEntity;

    @BeforeEach
    void setUp() {
        UsuarioEntity usuario = UsuarioEntity.builder()
                .id(1L)
                .nombre("Carlos")
                .curp("HELC950815HDFRPR03")
                .build();

        cuentaEntity = CuentaEntity.builder()
                .id(1L)
                .usuario(usuario)
                .numeroCuenta("1234567890123456")
                .estatus("ACTIVA")
                .saldoInicial(BigDecimal.ZERO)
                .saldoDisponible(BigDecimal.ZERO)
                .build();

        saldoEntity = SaldoEntity.builder()
                .id(1L)
                .usuario(usuario)
                .cuenta(cuentaEntity)
                .saldoActual(BigDecimal.ZERO)
                .saldoDisponible(BigDecimal.ZERO)
                .moneda("MXN")
                .build();
    }

    @Test
    @DisplayName("Debe consultar cuenta por número de cuenta exitosamente")
    void testConsultarPorNumeroCuenta() {
        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuentaEntity));
        CuentaResponse responseMock = CuentaResponse.builder()
                .cuenta(CuentaDTO.builder().numeroCuenta("1234567890123456").estatus("ACTIVA").build())
                .build();
        when(clienteMapper.toCuentaResponse(cuentaEntity)).thenReturn(responseMock);

        CuentaResponse response = cuentaService.consultarPorNumeroCuenta("1234567890123456");

        assertNotNull(response);
        assertEquals(CodigoRespuestaEnum.EXITO, response.getCodigoRespuesta());
        assertEquals(0, response.getCodigo());
        verify(cuentaRepository).findByNumeroCuenta("1234567890123456");
    }

    @Test
    @DisplayName("Debe lanzar CuentaNoEncontradaException cuando no existe el número de cuenta")
    void testConsultarPorNumeroCuentaNoEncontrada() {
        when(cuentaRepository.findByNumeroCuenta("9999999999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.consultarPorNumeroCuenta("9999999999999999"));
    }

    @Test
    @DisplayName("Debe consultar lista de cuentas activas")
    void testConsultarCuentasActivas() {
        when(cuentaRepository.findByEstatus("ACTIVA")).thenReturn(List.of(cuentaEntity));
        CuentaResponse responseMock = CuentaResponse.builder().build();
        when(clienteMapper.toCuentaResponse(any(CuentaEntity.class))).thenReturn(responseMock);

        List<CuentaResponse> response = cuentaService.consultarCuentasActivas();

        assertNotNull(response);
        assertEquals(1, response.size());
        verify(cuentaRepository).findByEstatus("ACTIVA");
    }

    @Test
    @DisplayName("Debe consultar saldo por número de cuenta exitosamente")
    void testConsultarSaldoPorNumeroCuenta() {
        when(cuentaRepository.findByNumeroCuenta("1234567890123456")).thenReturn(Optional.of(cuentaEntity));
        when(saldoRepository.findByCuentaId(1L)).thenReturn(Optional.of(saldoEntity));
        SaldoResponse responseMock = SaldoResponse.builder()
                .saldo(SaldoDTO.builder().saldoDisponible(BigDecimal.ZERO).moneda("MXN").build())
                .build();
        when(clienteMapper.toSaldoResponse(saldoEntity)).thenReturn(responseMock);

        SaldoResponse response = cuentaService.consultarSaldoPorNumeroCuenta("1234567890123456");

        assertNotNull(response);
        assertEquals(CodigoRespuestaEnum.EXITO, response.getCodigoRespuesta());
        assertEquals(0, response.getCodigo());
    }

    @Test
    @DisplayName("Debe generar número de cuenta único de exactamente 16 dígitos")
    void testGenerarNumeroCuentaUnico() {
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        String numeroCuenta = cuentaService.generarNumeroCuentaUnico();

        assertNotNull(numeroCuenta);
        assertEquals(16, numeroCuenta.length());
        assertTrue(numeroCuenta.matches("\\d{16}"));
    }
}
