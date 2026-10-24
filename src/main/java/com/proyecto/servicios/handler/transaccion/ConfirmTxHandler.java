package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.entity.transaccion.TransaccionEntity;
import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import com.proyecto.servicios.exception.TransaccionNoEncontradaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Handler de {@code confirmTx}: resuelve una transacción que quedó PENDIENTE_CONFIRMACION por timeout.
 *
 * <p>Reglas de la documentación: se envían exactamente los mismos parámetros de sendTx, no antes de 62 segundos
 * después del envío y nunca después de 24 horas.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
public class ConfirmTxHandler {

    private static final Duration VIGENCIA_CONFIRMACION = Duration.ofHours(24);

    private final RegistroTransaccion registro;
    private final CifradoPeticionHandler cifradoPeticionHandler;
    private final TokenHandler tokenHandler;
    private final EnvioTransaccionHandler envioTransaccionHandler;
    private final ResultadoTransaccionHandler resultadoTransaccionHandler;
    private final Clock clock;
    private final Duration esperaMinima;

    public ConfirmTxHandler(RegistroTransaccion registro,
                            CifradoPeticionHandler cifradoPeticionHandler,
                            TokenHandler tokenHandler,
                            EnvioTransaccionHandler envioTransaccionHandler,
                            ResultadoTransaccionHandler resultadoTransaccionHandler,
                            Clock clock,
                            @Value("${gestopago.transaccion.espera-confirmacion-segundos:62}") long esperaSegundos) {
        this.registro = registro;
        this.cifradoPeticionHandler = cifradoPeticionHandler;
        this.tokenHandler = tokenHandler;
        this.envioTransaccionHandler = envioTransaccionHandler;
        this.resultadoTransaccionHandler = resultadoTransaccionHandler;
        this.clock = clock;
        this.esperaMinima = Duration.ofSeconds(esperaSegundos);
    }

    public TransaccionResponse ejecutar(String upc, UsuarioAutenticado usuario) {
        TransaccionEntity tx = registro.buscarPorUpc(upc);
        if (!usuario.esEjecutivo() && !tx.getUsuario().getId().equals(usuario.clienteId())) {
            // Mismo error que si no existiera, para no revelar UPC de otros clientes
            throw new TransaccionNoEncontradaException("No existe una transacción con el UPC " + upc);
        }
        if (tx.getEstatus() == EstatusTransaccionEnum.PENDIENTE) {
            throw new ValidacionNegocioException("La transacción " + upc + " se está procesando; consulte nuevamente en unos segundos");
        }
        if (tx.getEstatus() != EstatusTransaccionEnum.PENDIENTE_CONFIRMACION) {
            throw new ValidacionNegocioException("La transacción " + upc + " ya está resuelta con estatus " + tx.getEstatus()
                    + "; no requiere confirmación");
        }
        validarVentanaDeConfirmacion(tx);

        ContextoTransaccion contexto = new ContextoTransaccion(null, usuario);
        contexto.setTransaccionId(tx.getId());
        contexto.setPayloadJson(tx.getPayloadEnviado());
        cifradoPeticionHandler.manejar(contexto);
        tokenHandler.manejar(contexto);
        log.info("Consultando confirmTx para la transacción {}", upc);
        envioTransaccionHandler.confirmar(contexto);
        resultadoTransaccionHandler.manejarConfirmacion(contexto);
        return contexto.getRespuesta();
    }

    private void validarVentanaDeConfirmacion(TransaccionEntity tx) {
        OffsetDateTime envio = tx.getFechaEnvio() != null ? tx.getFechaEnvio() : tx.getFechaCreacion();
        Duration transcurrido = Duration.between(envio, OffsetDateTime.now(clock));
        if (transcurrido.compareTo(esperaMinima) < 0) {
            long faltan = esperaMinima.minus(transcurrido).toSeconds() + 1;
            throw new ValidacionNegocioException(String.format(
                    "GestoPago solo permite confirmar una transacción %d segundos después de enviarla; intente en %d segundos",
                    esperaMinima.toSeconds(), faltan));
        }
        if (transcurrido.compareTo(VIGENCIA_CONFIRMACION) > 0) {
            throw new ValidacionNegocioException("La transacción se envió hace más de 24 horas; GestoPago ya no permite "
                    + "confirmarla. Contacte a soporte para conciliarla");
        }
    }
}
