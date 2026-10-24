package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.TokenGestoPagoException;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nodo «Obtener Token»: usa el token vigente de GestoPago (uno por día, guardado en base de datos) y solo pide uno
 * nuevo si no existe, venció o GestoPago lo rechazó (HTTP 403 "EXPIRED").
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
public class TokenHandler implements PasoTransaccionHandler {

    private final GestoPagoTokenService tokenService;
    private final Integer idDistribuidor;
    private final String codigoDispositivo;

    public TokenHandler(GestoPagoTokenService tokenService,
                        @Value("${gestopago.auth.id-distribuidor}") Integer idDistribuidor,
                        @Value("${gestopago.auth.codigo-dispositivo}") String codigoDispositivo) {
        this.tokenService = tokenService;
        this.idDistribuidor = idDistribuidor;
        this.codigoDispositivo = codigoDispositivo;
    }

    @Override
    public void manejar(ContextoTransaccion contexto) {
        contexto.setToken(tokenVigente());
    }

    /**
     * Fuerza un token nuevo; se usa cuando GestoPago responde que el actual expiró.
     */
    public void renovar(ContextoTransaccion contexto) {
        log.info("GestoPago rechazó el token vigente; se solicita uno nuevo");
        tokenService.renovarToken();
        contexto.setToken(tokenVigente());
    }

    private String tokenVigente() {
        return tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo)
                .map(GestoPagoToken::getToken)
                .filter(t -> !t.isBlank())
                .orElseThrow(() -> new TokenGestoPagoException(
                        "No fue posible obtener el token de autorización del procesador de pagos; intente más tarde"));
    }
}
