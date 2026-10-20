package com.proyecto.servicios.controller;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.ClienteIdRequest;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.model.common.PaginaResponse;
import com.proyecto.servicios.model.common.RespuestaApi;
import com.proyecto.servicios.security.ControlAcceso;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ClienteController")
class ClienteControllerTest {

    @Mock
    private ClienteService clienteService;

    @Mock
    private ControlAcceso controlAcceso;

    @InjectMocks
    private ClienteController clienteController;

    private ClienteResponse clienteResponse;

    @BeforeEach
    void setUp() {
        clienteResponse = ClienteResponse.builder()
                .id(1L)
                .nombre("Carlos")
                .curp("HELC950815HDFRPR03")
                .rfc("HELC950815ABC")
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/clientes debe registrar y responder 201 con la estructura uniforme")
    void testRegistrarCliente() {
        ClienteRegistroRequest request = ClienteRegistroRequest.builder().curp("HELC950815HDFRPR03").build();
        when(clienteService.registrarCliente(request)).thenReturn(clienteResponse);

        ResponseEntity<RespuestaApi<ClienteResponse>> response = clienteController.registrarCliente(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(0, response.getBody().getCodigo());
        assertEquals(CodigoRespuestaEnum.EXITO, response.getBody().getCodigoRespuesta());
        assertEquals("HELC950815HDFRPR03", response.getBody().getDatos().getCurp());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("POST /api/v1/clientes/buscar debe aplicar el control de acceso antes de buscar")
    void testBuscarClientes() {
        ClienteBusquedaRequest filtros = ClienteBusquedaRequest.builder().curp("HELC950815HDFRPR03").build();
        PaginaResponse<ClienteResponse> pagina = PaginaResponse.<ClienteResponse>builder()
                .contenido(List.of(clienteResponse)).pagina(0).tamanio(20).totalElementos(1).totalPaginas(1).build();
        when(controlAcceso.limitarBusquedaClientes(filtros)).thenReturn(filtros);
        when(clienteService.buscar(filtros)).thenReturn(pagina);

        ResponseEntity<RespuestaApi<PaginaResponse<ClienteResponse>>> response = clienteController.buscarClientes(filtros);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getDatos().getTotalElementos());
        assertEquals("Se encontraron 1 cliente(s)", response.getBody().getMensaje());
        var orden = inOrder(controlAcceso, clienteService);
        orden.verify(controlAcceso).limitarBusquedaClientes(filtros);
        orden.verify(clienteService).buscar(filtros);
    }

    @Test
    @DisplayName("POST /api/v1/clientes/buscar sin resultados debe responder 200 con mensaje específico")
    void testBuscarClientesSinResultados() {
        ClienteBusquedaRequest filtros = ClienteBusquedaRequest.builder().nombre("inexistente").build();
        when(controlAcceso.limitarBusquedaClientes(filtros)).thenReturn(filtros);
        when(clienteService.buscar(filtros)).thenReturn(PaginaResponse.<ClienteResponse>builder()
                .contenido(List.of()).pagina(0).tamanio(20).totalElementos(0).totalPaginas(0).build());

        ResponseEntity<RespuestaApi<PaginaResponse<ClienteResponse>>> response = clienteController.buscarClientes(filtros);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("No se encontraron clientes con los criterios indicados", response.getBody().getMensaje());
    }

    @Test
    @DisplayName("PUT /api/v1/clientes debe validar el acceso al id del cuerpo y actualizar")
    void testActualizarCliente() {
        ClienteUpdateRequest request = ClienteUpdateRequest.builder().id(1L).nombre("Carlos Eduardo").build();
        when(clienteService.actualizarCliente(request)).thenReturn(clienteResponse);

        ResponseEntity<RespuestaApi<ClienteResponse>> response = clienteController.actualizarCliente(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(controlAcceso).validarAccesoCliente(1L);
        verify(clienteService).actualizarCliente(request);
    }

    @Test
    @DisplayName("PUT /api/v1/clientes no debe ejecutar la lógica de negocio si el id pertenece a otro cliente")
    void testActualizarClienteAjeno() {
        ClienteUpdateRequest request = ClienteUpdateRequest.builder().id(2L).build();
        doThrow(new AccesoDenegadoException("sin permiso")).when(controlAcceso).validarAccesoCliente(2L);

        assertThrows(AccesoDenegadoException.class, () -> clienteController.actualizarCliente(request));
        verifyNoInteractions(clienteService);
    }

    @Test
    @DisplayName("DELETE /api/v1/clientes debe aplicar baja lógica con el id del cuerpo")
    void testEliminarClienteLogico() {
        clienteResponse.setActivo(false);
        when(clienteService.eliminarClienteLogico(1L)).thenReturn(clienteResponse);

        ResponseEntity<RespuestaApi<ClienteResponse>> response =
                clienteController.eliminarClienteLogico(ClienteIdRequest.builder().id(1L).build());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().getDatos().getActivo());
        verify(controlAcceso).validarAccesoCliente(1L);
    }
}
