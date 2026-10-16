package com.proyecto.servicios.security;

import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.util.PatronesValidacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ControlAcceso (autorización por registro)")
class ControlAccesoTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ControlAcceso controlAcceso;

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(Long clienteId, RolEnum rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(clienteId, "usuario", rol);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority(rol.authority()))));
    }

    private ClienteResponse clientePropio() {
        return ClienteResponse.builder()
                .id(1L)
                .curp("HELC950815HDFRPR03")
                .cuenta(CuentaDTO.builder().numeroCuenta("4152000000000001").build())
                .build();
    }

    @Test
    @DisplayName("Sin token debe lanzar NoAutenticadoException (401)")
    void sinTokenNoAutenticado() {
        assertThrows(NoAutenticadoException.class, () -> controlAcceso.validarAccesoCliente(1L));
    }

    @Test
    @DisplayName("Un cliente puede acceder a su propio ID")
    void clienteAccedeASuPropioId() {
        autenticar(1L, RolEnum.CLIENTE);
        assertDoesNotThrow(() -> controlAcceso.validarAccesoCliente(1L));
    }

    @Test
    @DisplayName("Un cliente NO puede acceder al ID de otro cliente cambiando el ID (IDOR)")
    void clienteNoAccedeAIdAjeno() {
        autenticar(1L, RolEnum.CLIENTE);
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class, () -> controlAcceso.validarAccesoCliente(2L));
        assertTrue(ex.getMessage().contains("No tiene permisos"));
    }

    @Test
    @DisplayName("El ejecutivo puede acceder a cualquier cliente sin consultar la base de datos")
    void ejecutivoAccedeACualquierCliente() {
        autenticar(null, RolEnum.EJECUTIVO);
        assertDoesNotThrow(() -> controlAcceso.validarAccesoCliente(99L));
        assertDoesNotThrow(() -> controlAcceso.validarRecursoPropio(c -> false, "la cuenta"));
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("Un cliente solo puede consultar su propia CURP y su propia cuenta")
    void recursoPropioPorCurpYCuenta() {
        autenticar(1L, RolEnum.CLIENTE);
        when(clienteService.consultarPorId(1L)).thenReturn(clientePropio());

        assertDoesNotThrow(() -> controlAcceso.validarRecursoPropio(
                c -> "helc950815hdfrpr03".equalsIgnoreCase(c.getCurp()), "el cliente"));
        assertDoesNotThrow(() -> controlAcceso.validarRecursoPropio(
                c -> PatronesValidacion.esCuentaDe(c, "4152000000000001"), "la cuenta"));
        assertThrows(AccesoDenegadoException.class, () -> controlAcceso.validarRecursoPropio(
                c -> PatronesValidacion.esCuentaDe(c, "4152999999999999"), "la cuenta"));
    }

    @Test
    @DisplayName("Las consultas masivas están reservadas para el ejecutivo")
    void consultasMasivasSoloEjecutivo() {
        autenticar(1L, RolEnum.CLIENTE);
        assertThrows(AccesoDenegadoException.class, () -> controlAcceso.validarRolEjecutivo());

        autenticar(null, RolEnum.EJECUTIVO);
        assertDoesNotThrow(() -> controlAcceso.validarRolEjecutivo());
    }
}
