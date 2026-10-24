package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.handler.transaccion.ConfirmTxHandler;
import com.proyecto.servicios.handler.transaccion.SendTxHandler;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.TransaccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Punto de entrada de negocio de las transacciones: delega cada operación en su handler.
 *
 * <p>No se marca como {@code @Transactional}: los handlers abren transacciones cortas propias para no mantener
 * bloqueada la cuenta mientras se espera la respuesta de GestoPago.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class TransaccionServiceImpl implements TransaccionService {

    private final SendTxHandler sendTxHandler;
    private final ConfirmTxHandler confirmTxHandler;

    @Override
    public TransaccionResponse enviar(TransaccionRequest request, UsuarioAutenticado usuario) {
        return sendTxHandler.ejecutar(request, usuario);
    }

    @Override
    public TransaccionResponse confirmar(String upc, UsuarioAutenticado usuario) {
        return confirmTxHandler.ejecutar(upc, usuario);
    }
}
