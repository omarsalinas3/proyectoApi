package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.model.common.PaginaResponse;

/**
 * Interfaz de servicio para el Onboarding y ciclo de vida de Clientes Personas Físicas.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
public interface ClienteService {

    /**
     * Registra un cliente nuevo, genera su cuenta bancaria y sus credenciales.
     *
     * @param request datos del cliente.
     * @return cliente registrado con su cuenta.
     */
    ClienteResponse registrarCliente(ClienteRegistroRequest request);

    /**
     * Actualiza los datos editables del cliente indicado en {@code request.id}.
     * CURP, RFC, número de cuenta, saldos y estatus no se modifican.
     *
     * @param request datos a actualizar.
     * @return cliente actualizado.
     */
    ClienteResponse actualizarCliente(ClienteUpdateRequest request);

    /**
     * Baja lógica: desactiva al cliente, cancela su cuenta y cierra su sesión.
     *
     * @param id identificador del cliente.
     * @return cliente dado de baja.
     */
    ClienteResponse eliminarClienteLogico(Long id);

    /**
     * Consulta un cliente por su identificador.
     *
     * @param id identificador del cliente.
     * @return cliente encontrado.
     * @throws com.proyecto.servicios.exception.ClienteNoEncontradoException si no existe.
     */
    ClienteResponse consultarPorId(Long id);

    /**
     * Busca clientes combinando los filtros informados (ID, CURP, RFC, correo, número de cuenta, nombre,
     * estatus y rango de fechas de registro). Sin filtros devuelve todos los clientes paginados.
     *
     * @param filtros criterios de búsqueda y paginación.
     * @return página de clientes que cumplen los filtros.
     */
    PaginaResponse<ClienteResponse> buscar(ClienteBusquedaRequest filtros);
}
