package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.request.VerificarSesionRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ControlAcceso controlAcceso;

    @InjectMocks
    private AuthController authController;

    private final UsuarioAutenticado cliente = new UsuarioAutenticado(1L, "carlos.h95", RolEnum.CLIENTE);

    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        loginResponse = LoginResponse.builder()
                .usuarioId(1L)
                .username("carlos.h95")
                .sesionActiva(true)
                .tiempoInactividadSegundos(300)
                .token("token.jwt")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login debe autenticar y devolver el token en datos")
    void testLogin() {
        LoginRequest request = LoginRequest.builder().username("carlos.h95").password("Pass123!").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        ResponseEntity<RespuestaApi<LoginResponse>> response = authController.login(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals("token.jwt", response.getBody().getDatos().getToken());
        verify(authService).login(request);
    }

    @Test
    @DisplayName("POST /api/v1/auth/sesion/verificar debe verificar la sesión del usuario del token")
    void testVerificarSesion() {
        when(controlAcceso.usuarioActual()).thenReturn(cliente);
        when(authService.verificarEstadoSesion(cliente, 300)).thenReturn(loginResponse);

        ResponseEntity<RespuestaApi<LoginResponse>> response =
                authController.verificarSesion(VerificarSesionRequest.builder().timeoutDinamico(300).build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getDatos().getSesionActiva());
    }

    @Test
    @DisplayName("POST /api/v1/auth/sesion/verificar acepta el cuerpo vacío")
    void testVerificarSesionSinCuerpo() {
        when(controlAcceso.usuarioActual()).thenReturn(cliente);
        when(authService.verificarEstadoSesion(cliente, null)).thenReturn(loginResponse);

        ResponseEntity<RespuestaApi<LoginResponse>> response = authController.verificarSesion(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(authService).verificarEstadoSesion(cliente, null);
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout debe cerrar la sesión del usuario del token")
    void testLogout() {
        loginResponse.setSesionActiva(false);
        when(controlAcceso.usuarioActual()).thenReturn(cliente);
        when(authService.logout(cliente)).thenReturn(loginResponse);

        ResponseEntity<RespuestaApi<LoginResponse>> response = authController.logout();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getDatos().getSesionActiva());
        verify(authService).logout(cliente);
    }
}
