package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.common.PaginaResponse;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
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
    @DisplayName("Debe buscar cuentas con filtros combinados y devolver el resultado paginado")
    void testBuscarCuentas() {
        CuentaResponse responseMock = CuentaResponse.builder()
                .cuenta(CuentaDTO.builder().numeroCuenta("1234567890123456").estatus("ACTIVA").build())
                .build();
        when(cuentaRepository.findAll(org.mockito.ArgumentMatchers.<Specification<CuentaEntity>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cuentaEntity), PageRequest.of(0, 20), 1));
        when(clienteMapper.toCuentaResponse(cuentaEntity)).thenReturn(responseMock);

        CuentaBusquedaRequest filtros = CuentaBusquedaRequest.builder().estatus("ACTIVA").clienteId(1L).build();
        PaginaResponse<CuentaResponse> pagina = cuentaService.buscar(filtros);

        assertEquals(1, pagina.getTotalElementos());
        assertEquals(1, pagina.getContenido().size());
        assertEquals("1234567890123456", pagina.getContenido().get(0).getCuenta().getNumeroCuenta());
        ArgumentCaptor<Pageable> paginaCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(cuentaRepository).findAll(org.mockito.ArgumentMatchers.<Specification<CuentaEntity>>any(), paginaCaptor.capture());
        assertEquals(0, paginaCaptor.getValue().getPageNumber());
        assertEquals(20, paginaCaptor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Debe lanzar CuentaNoEncontradaException al consultar el saldo de una cuenta inexistente")
    void testConsultarSaldoCuentaNoEncontrada() {
        when(cuentaRepository.findByNumeroCuenta("9999999999999999")).thenReturn(Optional.empty());

        assertThrows(CuentaNoEncontradaException.class,
                () -> cuentaService.consultarSaldoPorNumeroCuenta("9999999999999999"));
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
        assertEquals("MXN", response.getSaldo().getMoneda());
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
