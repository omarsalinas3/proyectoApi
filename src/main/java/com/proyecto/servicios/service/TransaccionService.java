package com.proyecto.servicios.service;

import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;

/**
 * Transacciones de pago/venta con GestoPago.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface TransaccionService {

    /**
     * Ejecuta el flujo completo de sendTx (validaciones, cifrado, token, envío y registro).
     */
    TransaccionResponse enviar(TransaccionRequest request, UsuarioAutenticado usuario);

    /**
     * Consulta con confirmTx el resultado de una transacción que quedó pendiente por timeout.
     */
    TransaccionResponse confirmar(String upc, UsuarioAutenticado usuario);
}
