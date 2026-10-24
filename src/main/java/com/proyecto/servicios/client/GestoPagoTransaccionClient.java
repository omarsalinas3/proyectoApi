package com.proyecto.servicios.client;

import com.proyecto.servicios.client.config.GestoPagoTransaccionFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * Cliente Feign de las operaciones de venta/pago de GestoPago (PuntoRed).
 *
 * <p>Ambos métodos reciben un formulario {@code application/x-www-form-urlencoded} con {@code random} (cadena
 * aleatoria, normalmente el timestamp) y {@code signed} (payload JSON cifrado con AES-256-CBC), y el token en el
 * encabezado {@code Authorization: Bearer}. Responden XML.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@FeignClient(
        name = "gestoPagoTransacciones",
        url = "${gestopago.transaccion.url}",
        configuration = GestoPagoTransaccionFeignConfig.class
)
public interface GestoPagoTransaccionClient {

    String PARAM_RANDOM = "random";
    String PARAM_SIGNED = "signed";

    @PostMapping(value = "${gestopago.transaccion.endpoint.send-tx}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String sendTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion, @RequestBody Map<String, ?> formulario);

    @PostMapping(value = "${gestopago.transaccion.endpoint.confirm-tx}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    String confirmTx(@RequestHeader(HttpHeaders.AUTHORIZATION) String autorizacion, @RequestBody Map<String, ?> formulario);
}
