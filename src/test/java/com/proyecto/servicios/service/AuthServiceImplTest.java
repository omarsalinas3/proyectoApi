package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.service.Impl.AuthServiceImpl;
import com.proyecto.servicios.util.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - AuthServiceImpl")
class AuthServiceImplTest {

    @Mock
    private LoginRepository loginRepository;

    @Mock
    private CryptoService cryptoService;

    @Mock
    private BiometricService biometricService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginEntity loginEntity;
    private UsuarioEntity usuarioEntity;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "defaultTimeoutSeconds", 300);

        usuarioEntity = UsuarioEntity.builder()
                .id(1L)
                .nombre("Carlos")
                .apellidoPaterno("Hernandez")
                .build();

        loginEntity = LoginEntity.builder()
                .id(1L)
                .usuario(usuarioEntity)
                .usernameCifrado("enc_carlos.h95")
                .passwordHash("hashed_password_123")
                .datosBiometricosCifrados("enc_biometrics_123")
                .datosBiometricosPrecision(new BigDecimal("0.987654"))
                .sesionActiva(false)
                .tiempoInactividadSegundos(300)
                .ultimaActividad(OffsetDateTime.now())
                .fechaCreacion(OffsetDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debe iniciar sesión exitosamente cuando las credenciales son válidas")
    void testLoginExitoso() {
        LoginRequest request = LoginRequest.builder()
                .bandera(1)
                .username("carlos.h95")
                .password("Password123!")
                .build();

        when(cryptoService.encrypt("carlos.h95")).thenReturn("enc_carlos.h95");
        when(loginRepository.findByUsernameCifrado("enc_carlos.h95")).thenReturn(Optional.of(loginEntity));
        when(cryptoService.verifyPassword("Password123!", "hashed_password_123")).thenReturn(true);
        when(loginRepository.save(any(LoginEntity.class))).thenReturn(loginEntity);

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals(CodigoRespuestaEnum.EXITO, response.getCodigoRespuesta());
        assertEquals(0, response.getCodigo());
        assertTrue(response.getSesionActiva());
        assertEquals("carlos.h95", response.getUsername());
        assertEquals(300, response.getTiempoInactividadSegundos());
        verify(loginRepository).save(any(LoginEntity.class));
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException cuando la contraseña no coincide")
    void testLoginContrasenaInvalida() {
        LoginRequest request = LoginRequest.builder()
                .username("carlos.h95")
                .password("PasswordInvalido")
                .build();

        when(cryptoService.encrypt("carlos.h95")).thenReturn("enc_carlos.h95");
        when(loginRepository.findByUsernameCifrado("enc_carlos.h95")).thenReturn(Optional.of(loginEntity));
        when(cryptoService.verifyPassword("PasswordInvalido", "hashed_password_123")).thenReturn(false);

        assertThrows(CredencialesInvalidasException.class, () -> authService.login(request));
        verify(loginRepository, never()).save(any(LoginEntity.class));
    }

    @Test
    @DisplayName("Debe validar vigencia de sesión activa correctamente dentro del tiempo límite")
    void testVerificarEstadoSesionValida() {
        loginEntity.setSesionActiva(true);
        loginEntity.setUltimaActividad(OffsetDateTime.now().minusSeconds(10));

        when(cryptoService.encrypt("carlos.h95")).thenReturn("enc_carlos.h95");
        when(loginRepository.findByUsernameCifrado("enc_carlos.h95")).thenReturn(Optional.of(loginEntity));
        when(loginRepository.save(any(LoginEntity.class))).thenReturn(loginEntity);

        LoginResponse response = authService.verificarEstadoSesion("carlos.h95", 300);

        assertNotNull(response);
        assertTrue(response.getSesionActiva());
        assertEquals("carlos.h95", response.getUsername());
        verify(loginRepository).save(any(LoginEntity.class));
    }

    @Test
    @DisplayName("Debe desactivar sesión (sesionActiva = false) y lanzar SesionExpiradaException cuando supera el timeout dinámico")
    void testVerificarEstadoSesionExpirada() {
        loginEntity.setSesionActiva(true);
        loginEntity.setUltimaActividad(OffsetDateTime.now().minusSeconds(350)); // 350 segundos de inactividad

        when(cryptoService.encrypt("carlos.h95")).thenReturn("enc_carlos.h95");
        when(loginRepository.findByUsernameCifrado("enc_carlos.h95")).thenReturn(Optional.of(loginEntity));

        assertThrows(SesionExpiradaException.class, () -> authService.verificarEstadoSesion("carlos.h95", 300));
        assertFalse(loginEntity.getSesionActiva());
        verify(loginRepository).save(loginEntity);
    }

    @Test
    @DisplayName("Debe cerrar sesión correctamente (logout)")
    void testLogoutExitoso() {
        loginEntity.setSesionActiva(true);

        when(cryptoService.encrypt("carlos.h95")).thenReturn("enc_carlos.h95");
        when(loginRepository.findByUsernameCifrado("enc_carlos.h95")).thenReturn(Optional.of(loginEntity));
        when(loginRepository.save(any(LoginEntity.class))).thenReturn(loginEntity);

        LoginResponse response = authService.logout("carlos.h95");

        assertNotNull(response);
        assertFalse(response.getSesionActiva());
        assertEquals(0, response.getCodigo());
        verify(loginRepository).save(loginEntity);
    }
}
