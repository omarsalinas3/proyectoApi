package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.UsuarioDuplicadoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import com.proyecto.servicios.util.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ClienteServiceImpl")
class ClienteServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private CuentaService cuentaService;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private BiometricService biometricService;

    @Mock
    private ClienteMapper clienteMapper;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private ClienteRegistroRequest registroRequest;
    private UsuarioEntity usuarioEntity;

    @BeforeEach
    void setUp() {
        DomicilioDTO domicilioDTO = DomicilioDTO.builder()
                .calle("Av. Reforma")
                .numeroExterior("123")
                .colonia("Juárez")
                .codigoPostal("06600")
                .municipio("Cuauhtémoc")
                .estado("CDMX")
                .pais("México")
                .build();

        registroRequest = ClienteRegistroRequest.builder()
                .nombre("Carlos")
                .apellidoPaterno("Hernández")
                .apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 8, 15))
                .curp("HELC950815HDFRPR03")
                .rfc("HELC950815ABC")
                .sexo("M")
                .nacionalidad("Mexicana")
                .estadoCivil("SOLTERO")
                .correo("carlos.hernandez@example.com")
                .telefonoMovil("5512345678")
                .ocupacion("Ingeniero")
                .empresa("Tech Solutions")
                .ingresoMensual(new BigDecimal("35000.00"))
                .domicilio(domicilioDTO)
                .username("carlos.h95")
                .password("PasswordSeguro123!")
                .datosBiometricos(new BigDecimal("0.987654"))
                .build();

        usuarioEntity = UsuarioEntity.builder()
                .id(1L)
                .nombre("Carlos")
                .apellidoPaterno("Hernández")
                .apellidoMaterno("López")
                .curp("HELC950815HDFRPR03")
                .rfc("HELC950815ABC")
                .correo("carlos.hernandez@example.com")
                .activo(true)
                .fechaRegistro(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe registrar un cliente nuevo y generar automáticamente cuenta de 16 dígitos y saldo $0.00")
    void testRegistrarClienteExitoso() {
        when(usuarioRepository.existsByCurp(anyString())).thenReturn(false);
        when(usuarioRepository.existsByRfc(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo(anyString())).thenReturn(false);
        when(cryptoService.encrypt(anyString())).thenReturn("enc_username");
        when(loginRepository.existsByUsernameCifrado("enc_username")).thenReturn(false);
        when(clienteMapper.requestToEntity(registroRequest)).thenReturn(usuarioEntity);
        when(clienteMapper.domicilioDtoToEntity(any())).thenReturn(new DomicilioEntity());
        when(cuentaService.generarNumeroCuentaUnico()).thenReturn("1234567890123456");
        when(cryptoService.hashPassword(anyString())).thenReturn("hashed_password");
        when(biometricService.cifrarVectorEmbedding(any(), any())).thenReturn("enc_biometrics");
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(usuarioEntity);

        ClienteResponse responseMock = ClienteResponse.builder()
                .id(1L)
                .nombre("Carlos")
                .curp("HELC950815HDFRPR03")
                .build();
        when(clienteMapper.entityToResponse(usuarioEntity)).thenReturn(responseMock);

        ClienteResponse response = clienteService.registrarCliente(registroRequest);

        assertNotNull(response);
        assertEquals(CodigoRespuestaEnum.EXITO, response.getCodigoRespuesta());
        assertEquals(0, response.getCodigo());
        verify(cuentaService).generarNumeroCuentaUnico();
        verify(usuarioRepository).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe rechazar el registro de un cliente menor de 18 años")
    void testRegistrarClienteMenorDeEdad() {
        registroRequest.setFechaNacimiento(LocalDate.now().minusYears(17));

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> clienteService.registrarCliente(registroRequest));

        assertTrue(ex.getMessage().contains("mayor de edad"));
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe rechazar el registro cuando el ingreso mensual es menor o igual a cero")
    void testRegistrarClienteIngresoInvalido() {
        registroRequest.setIngresoMensual(BigDecimal.ZERO);

        ValidacionNegocioException ex = assertThrows(ValidacionNegocioException.class,
                () -> clienteService.registrarCliente(registroRequest));

        assertTrue(ex.getMessage().contains("ingreso mensual"));
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar CurpDuplicadaException cuando la CURP ya existe")
    void testRegistrarClienteCurpDuplicada() {
        when(usuarioRepository.existsByCurp("HELC950815HDFRPR03")).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> clienteService.registrarCliente(registroRequest));
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar RfcDuplicadoException cuando el RFC ya existe")
    void testRegistrarClienteRfcDuplicado() {
        when(usuarioRepository.existsByCurp(anyString())).thenReturn(false);
        when(usuarioRepository.existsByRfc("HELC950815ABC")).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> clienteService.registrarCliente(registroRequest));
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar CorreoDuplicadoException cuando el correo ya existe")
    void testRegistrarClienteCorreoDuplicado() {
        when(usuarioRepository.existsByCurp(anyString())).thenReturn(false);
        when(usuarioRepository.existsByRfc(anyString())).thenReturn(false);
        when(usuarioRepository.existsByCorreo("carlos.hernandez@example.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> clienteService.registrarCliente(registroRequest));
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("Debe actualizar información del cliente exitosamente")
    void testActualizarClienteExitoso() {
        ClienteUpdateRequest updateRequest = ClienteUpdateRequest.builder()
                .nombre("Carlos Eduardo")
                .correo("carlos.nuevo@example.com")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.existsByCorreoAndIdNot("carlos.nuevo@example.com", 1L)).thenReturn(false);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(usuarioEntity);

        ClienteResponse responseMock = ClienteResponse.builder().id(1L).build();
        when(clienteMapper.entityToResponse(usuarioEntity)).thenReturn(responseMock);

        ClienteResponse response = clienteService.actualizarCliente(1L, updateRequest);

        assertNotNull(response);
        assertEquals(CodigoRespuestaEnum.EXITO, response.getCodigoRespuesta());
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("Debe ejecutar baja lógica correctamente cambiando activo a false y cancelando cuenta")
    void testEliminarClienteLogico() {
        CuentaEntity cuenta = CuentaEntity.builder().estatus("ACTIVA").build();
        LoginEntity login = LoginEntity.builder().sesionActiva(true).build();
        usuarioEntity.setCuenta(cuenta);
        usuarioEntity.setLogin(login);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(usuarioEntity);

        ClienteResponse responseMock = ClienteResponse.builder().id(1L).activo(false).build();
        when(clienteMapper.entityToResponse(usuarioEntity)).thenReturn(responseMock);

        ClienteResponse response = clienteService.eliminarClienteLogico(1L);

        assertNotNull(response);
        assertFalse(usuarioEntity.getActivo());
        assertEquals("CANCELADA", cuenta.getEstatus());
        assertFalse(login.getSesionActiva());
        verify(usuarioRepository).save(usuarioEntity);
    }

    @Test
    @DisplayName("Debe consultar cliente por ID exitosamente")
    void testConsultarPorId() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioEntity));
        ClienteResponse responseMock = ClienteResponse.builder().id(1L).build();
        when(clienteMapper.entityToResponse(usuarioEntity)).thenReturn(responseMock);

        ClienteResponse response = clienteService.consultarPorId(1L);

        assertNotNull(response);
        assertEquals(0, response.getCodigo());
    }

    @Test
    @DisplayName("Debe lanzar ClienteNoEncontradoException cuando el ID no existe")
    void testConsultarPorIdNoEncontrado() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> clienteService.consultarPorId(99L));
    }
}
