package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.service.DosFactoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Nodo «2FA»: valida el código TOTP enviado en la petición y lo marca como usado.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class DosFactoresHandler implements PasoTransaccionHandler {

    private final DosFactoresService dosFactoresService;

    @Override
    public void manejar(ContextoTransaccion contexto) {
        dosFactoresService.validarCodigo(contexto.getClienteId(), contexto.getRequest().getCodigo2fa());
    }
}
