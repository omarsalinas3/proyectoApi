package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.exception.TransaccionPendienteException;
import com.proyecto.servicios.exception.TransaccionRechazadaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nodo «¡Pago Exitoso!»: registra en base de datos el resultado de GestoPago y afecta el saldo.
 *
 * <ul>
 *   <li>CODIGO 01: transacción EXITOSA, se carga el monto retenido.</li>
 *   <li>CODIGO 82 o sin respuesta: PENDIENTE_CONFIRMACION, el monto sigue retenido.</li>
 *   <li>Cualquier otro código: RECHAZADA, se libera el monto.</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class ResultadoTransaccionHandler implements PasoTransaccionHandler {

    private final RegistroTransaccion registro;

    @Value("${gestopago.transaccion.espera-confirmacion-segundos:62}")
    private long esperaConfirmacionSegundos;

    @Override
    public void manejar(ContextoTransaccion contexto) {
        contexto.setRespuesta(resolver(contexto, false));
    }

    /**
     * Interpreta la respuesta de confirmTx: la transacción se considera aplicada con código 01/06 y número de
     * autorización; con código 70 no fue aplicada.
     */
    public void manejarConfirmacion(ContextoTransaccion contexto) {
        contexto.setRespuesta(resolver(contexto, true));
    }

    private TransaccionResponse resolver(ContextoTransaccion contexto, boolean esConfirmacion) {
        GestoPagoTxResultado resultado = contexto.getResultado();
        Long id = contexto.getTransaccionId();
        String xml = contexto.getRespuestaXml();

        boolean aplicada = resultado != null && (esConfirmacion ? resultado.confirmaAplicada() : resultado.esExitosa());
        if (aplicada) {
            return registro.registrarExitosa(id, resultado, xml);
        }
        boolean noAplicada = resultado != null && !resultado.esTiempoDeEsperaAgotado()
                && (!esConfirmacion || GestoPagoTxResultado.CODIGO_NO_APLICADA.equals(resultado.getCodigo()));
        if (noAplicada) {
            TransaccionResponse rechazada = registro.registrarRechazada(id, resultado, xml);
            throw new TransaccionRechazadaException(rechazada.getUpc(), resultado.getCodigo(), String.format(
                    "GestoPago rechazó la transacción %s (código %s): %s. El monto no fue cargado a la cuenta",
                    rechazada.getUpc(), resultado.getCodigo(), textoLimpio(resultado.getTexto())));
        }
        TransaccionResponse pendiente = registro.registrarPendiente(id, resultado, xml);
        throw new TransaccionPendienteException(pendiente.getUpc(), String.format(
                "GestoPago no confirmó el resultado de la transacción %s; el monto queda retenido. Consulte su estado en "
                        + "POST /api/v1/transacciones/confirmar con {\"upc\":\"%s\"} después de %d segundos",
                pendiente.getUpc(), pendiente.getUpc(), esperaConfirmacionSegundos));
    }

    private static String textoLimpio(String texto) {
        return texto == null ? "sin descripción" : texto.replaceAll("\\s+", " ").trim();
    }
}
