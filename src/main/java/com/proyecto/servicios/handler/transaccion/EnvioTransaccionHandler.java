package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.client.GestoPagoTransaccionClient;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.ExternalServiceTimeoutException;
import com.proyecto.servicios.exception.TokenGestoPagoException;
import com.proyecto.servicios.util.GestoPagoXmlParser;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Nodo «Sen Tx Cifrado + Token»: envía el formulario {@code random} + {@code signed} con el token en
 * {@code Authorization: Bearer} y deja la respuesta interpretada en el contexto.
 *
 * <ul>
 *   <li>403/401 (token expirado): se renueva el token y se reintenta una sola vez; la operación no fue procesada.</li>
 *   <li>4xx distinto de 401/403: GestoPago no procesó la operación; se libera el monto y se informa el error.</li>
 *   <li>Timeout, 5xx o respuesta ilegible: el resultado es incierto, así que el contexto queda sin resultado y el
 *   registro la marcará PENDIENTE_CONFIRMACION (el monto sigue retenido hasta consultar confirmTx).</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioTransaccionHandler implements PasoTransaccionHandler {

    private final GestoPagoTransaccionClient cliente;
    private final TokenHandler tokenHandler;
    private final GestoPagoXmlParser xmlParser;
    private final RegistroTransaccion registro;

    @Override
    public void manejar(ContextoTransaccion contexto) {
        ejecutar(contexto, cliente::sendTx, "sendTx", true);
    }

    /**
     * Mismo envío contra {@code confirmTx.do} (con exactamente el mismo payload que se usó en sendTx). Si la consulta
     * falla, la transacción original sigue sin resultado conocido, así que nunca se libera el monto aquí.
     */
    public void confirmar(ContextoTransaccion contexto) {
        ejecutar(contexto, cliente::confirmTx, "confirmTx", false);
    }

    private void ejecutar(ContextoTransaccion contexto, BiFunction<String, Map<String, ?>, String> operacion, String nombre,
                          boolean liberarSiNoSeProceso) {
        Map<String, String> formulario = Map.of(
                GestoPagoTransaccionClient.PARAM_RANDOM, contexto.getRandom(),
                GestoPagoTransaccionClient.PARAM_SIGNED, contexto.getPeticionCifrada());
        String upc = contexto.getPayload() != null ? contexto.getPayload().getUpc() : String.valueOf(contexto.getTransaccionId());
        try {
            String xml = enviarRenovandoToken(contexto, operacion, formulario);
            contexto.setRespuestaXml(xml);
            log.info("{} {} respondió: {}", nombre, upc, xml == null ? "(vacío)" : xml.replaceAll("\\s+", " "));
            contexto.setResultado(xmlParser.parsear(xml));
        } catch (TokenGestoPagoException e) {
            if (liberarSiNoSeProceso) {
                registro.registrarRechazada(contexto.getTransaccionId(), null, null);
            }
            throw e;
        } catch (ExternalServiceException e) {
            if (e.getStatusCode() >= 400 && e.getStatusCode() < 500) {
                if (liberarSiNoSeProceso) {
                    registro.registrarRechazada(contexto.getTransaccionId(), null, null);
                }
                throw new ExternalServiceException("El procesador de pagos rechazó la petición " + nombre + " (HTTP "
                        + e.getStatusCode() + ")" + (liberarSiNoSeProceso ? "; el monto no fue cargado" : ""), e, e.getStatusCode());
            }
            log.error("{} {} sin resultado confirmado (HTTP {} / respuesta inválida): {}", nombre, upc, e.getStatusCode(), e.getMessage());
            contexto.setResultado(null);
        } catch (FeignException | ExternalServiceTimeoutException e) {
            // RetryableException (timeout de lectura, conexión cortada) es un FeignException
            log.error("{} {} sin respuesta de GestoPago ({}): queda pendiente de confirmación", nombre, upc, e.getClass().getSimpleName());
            contexto.setResultado(null);
        }
    }

    private String enviarRenovandoToken(ContextoTransaccion contexto, BiFunction<String, Map<String, ?>, String> operacion,
                                        Map<String, String> formulario) {
        try {
            return operacion.apply("Bearer " + contexto.getToken(), formulario);
        } catch (ExternalServiceAuthException expirado) {
            tokenHandler.renovar(contexto);
            try {
                return operacion.apply("Bearer " + contexto.getToken(), formulario);
            } catch (ExternalServiceAuthException rechazado) {
                throw new TokenGestoPagoException("El procesador de pagos rechazó el token de autorización; el monto no fue cargado");
            }
        }
    }
}
