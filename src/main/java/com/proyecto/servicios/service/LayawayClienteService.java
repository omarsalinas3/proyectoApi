package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.LayawayClienteRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;

/**
 * Servicio unificado para la gestión integral de clientes mediante banderas de operación.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface LayawayClienteService {

    /**
     * Procesa la operación unificada sobre el cliente:
     * <ul>
     *   <li>Bandera 1: Registro completo y apertura de cuenta bancaria automática.</li>
     *   <li>Bandera 2: Actualización de información personal, contacto y domicilio protegiendo CURP/RFC.</li>
     *   <li>Bandera 3: Baja lógica del cliente y cancelación de cuentas asociadas.</li>
     * </ul>
     *
     * @param request petición unificada con bandera y datos correspondientes
     * @return respuesta estandarizada con los datos del cliente y estado de la operación
     */
    ClienteResponse procesarOperacionCliente(LayawayClienteRequest request);
}
