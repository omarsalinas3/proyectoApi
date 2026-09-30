package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ClienteController")
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteResponse clienteResponse;

    @BeforeEach
    void setUp() {
        clienteResponse = ClienteResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Operación realizada con éxito")
                .id(1L)
                .nombre("Carlos")
                .curp("HELC950815HDFRPR03")
                .rfc("HELC950815ABC")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/clientes debe registrar cliente y retornar HTTP 201 Created con código 0")
    void testRegistrarCliente() {
        ClienteRegistroRequest request = ClienteRegistroRequest.builder()
                .curp("HELC950815HDFRPR03")
                .build();

        when(clienteService.registrarCliente(any(ClienteRegistroRequest.class))).thenReturn(clienteResponse);

        ResponseEntity<ClienteResponse> response = clienteController.registrarCliente(request);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getCodigo());
        verify(clienteService).registrarCliente(request);
    }

    @Test
    @DisplayName("PUT /api/v1/clientes/{id} debe actualizar cliente y retornar HTTP 200 OK")
    void testActualizarCliente() {
        ClienteUpdateRequest request = ClienteUpdateRequest.builder().nombre("Carlos").build();
        when(clienteService.actualizarCliente(eq(1L), any(ClienteUpdateRequest.class))).thenReturn(clienteResponse);

        ResponseEntity<ClienteResponse> response = clienteController.actualizarCliente(1L, request);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        verify(clienteService).actualizarCliente(1L, request);
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes/{id} debe aplicar baja lógica y retornar HTTP 200 OK")
    void testEliminarClienteLogico() {
        clienteResponse.setActivo(false);
        when(clienteService.eliminarClienteLogico(1L)).thenReturn(clienteResponse);

        ResponseEntity<ClienteResponse> response = clienteController.eliminarClienteLogico(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getActivo());
        verify(clienteService).eliminarClienteLogico(1L);
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} debe retornar cliente por ID")
    void testConsultarPorId() {
        when(clienteService.consultarPorId(1L)).thenReturn(clienteResponse);

        ResponseEntity<ClienteResponse> response = clienteController.consultarPorId(1L);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1L, response.getBody().getId());
    }

    @Test
    @DisplayName("GET /api/v1/clientes/curp/{curp} debe retornar cliente por CURP")
    void testConsultarPorCurp() {
        when(clienteService.consultarPorCurp("HELC950815HDFRPR03")).thenReturn(clienteResponse);

        ResponseEntity<ClienteResponse> response = clienteController.consultarPorCurp("HELC950815HDFRPR03");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("HELC950815HDFRPR03", response.getBody().getCurp());
    }

    @Test
    @DisplayName("GET /api/v1/clientes debe retornar listado de clientes")
    void testConsultarTodos() {
        when(clienteService.consultarTodos(true)).thenReturn(List.of(clienteResponse));

        ResponseEntity<List<ClienteResponse>> response = clienteController.consultarTodos(true);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    @DisplayName("GET /api/v1/clientes/rango-fechas debe retornar clientes por rango")
    void testConsultarPorRangoFechas() {
        LocalDate inicio = LocalDate.of(2026, 1, 1);
        LocalDate fin = LocalDate.of(2026, 12, 31);
        when(clienteService.consultarPorRangoFechas(inicio, fin)).thenReturn(List.of(clienteResponse));

        ResponseEntity<List<ClienteResponse>> response = clienteController.consultarPorRangoFechas(inicio, fin);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }
}
