package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.AccesoDenegadoException;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - AuthController")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private ControlAcceso controlAcceso;

    private final UsuarioAutenticado cliente = new UsuarioAutenticado(1L, "carlos.h95", RolEnum.CLIENTE);

    @InjectMocks
    private AuthController authController;

    private LoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        loginResponse = LoginResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Inicio de sesión exitoso")
                .username("carlos.h95")
                .sesionActiva(true)
                .tiempoInactividadSegundos(300)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login debe autenticar y retornar HTTP 200 con sesión activa")
    void testLogin() {
        LoginRequest request = LoginRequest.builder().username("carlos.h95").password("Pass123!").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = authController.login(request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSesionActiva());
        assertEquals(0, response.getBody().getCodigo());
        verify(authService).login(request);
    }

    @Test
    @DisplayName("GET /api/v1/auth/sesion/verificar debe verificar estado de sesión")
    void testVerificarSesion() {
        when(controlAcceso.usuarioActual()).thenReturn(cliente);
        when(authService.verificarEstadoSesion(eq(cliente), eq(300))).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = authController.verificarSesion("carlos.h95", 300);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getSesionActiva());
        verify(authService).verificarEstadoSesion(cliente, 300);
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout debe cerrar sesión exitosamente")
    void testLogout() {
        loginResponse.setSesionActiva(false);
        when(controlAcceso.usuarioActual()).thenReturn(cliente);
        when(authService.logout(cliente)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> response = authController.logout("carlos.h95");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getSesionActiva());
        verify(authService).logout(cliente);
    }

    @Test
    @DisplayName("No debe permitir consultar ni cerrar la sesión de otro usuario")
    void testNoPermiteSesionDeTerceros() {
        when(controlAcceso.usuarioActual()).thenReturn(cliente);

        assertThrows(AccesoDenegadoException.class, () -> authController.verificarSesion("otro.usuario", null));
        assertThrows(AccesoDenegadoException.class, () -> authController.logout("otro.usuario"));
        verifyNoInteractions(authService);
    }
}
