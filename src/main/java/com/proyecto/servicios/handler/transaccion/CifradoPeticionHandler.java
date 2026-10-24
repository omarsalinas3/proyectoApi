package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.security.gestopago.GestoPagoCifrador;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Nodo «Cifrar Petición»: cifra el JSON con AES-256-CBC (parámetro {@code signed}) y genera el parámetro
 * {@code random} (el timestamp actual, como sugiere la documentación).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CifradoPeticionHandler implements PasoTransaccionHandler {

    private final GestoPagoCifrador cifrador;
    private final Clock clock;

    @Override
    public void manejar(ContextoTransaccion contexto) {
        contexto.setRandom(String.valueOf(clock.millis()));
        contexto.setPeticionCifrada(cifrador.cifrar(contexto.getPayloadJson()));
    }
}
