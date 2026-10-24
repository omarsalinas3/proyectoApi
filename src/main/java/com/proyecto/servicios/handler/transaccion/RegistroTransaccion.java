package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.transaccion.TransaccionEntity;
import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.TransaccionNoEncontradaException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Persistencia del ciclo de vida de una transacción y de su efecto en el saldo.
 *
 * <ul>
 *   <li>{@link #reservar}: con la cuenta bloqueada vuelve a validar el saldo, lo retiene y registra la transacción
 *   PENDIENTE con el payload (evidencia obligatoria) antes de enviarla.</li>
 *   <li>{@link #registrarExitosa}: carga definitivamente el monto retenido.</li>
 *   <li>{@link #registrarRechazada}: devuelve el monto retenido al saldo disponible.</li>
 *   <li>{@link #registrarPendiente}: conserva la retención hasta resolverla con confirmTx.</li>
 * </ul>
 *
 * <p>Cada método es una transacción corta: la fila de la cuenta nunca queda bloqueada mientras se espera la
 * respuesta de GestoPago.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegistroTransaccion {

    private final CuentaRepository cuentaRepository;
    private final TransaccionRepository transaccionRepository;
    private final Clock clock;

    @Transactional
    public void reservar(ContextoTransaccion contexto) {
        BigDecimal monto = contexto.getRequest().getMonto().setScale(2, RoundingMode.HALF_UP);
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuentaParaActualizar(contexto.getRequest().getNumCuenta())
                .orElseThrow(() -> new ClienteInactivoException("La cuenta ya no está disponible para operar"));
        if (!ClienteActivoHandler.ESTATUS_CUENTA_ACTIVA.equals(cuenta.getEstatus())) {
            throw new ClienteInactivoException("La cuenta " + cuenta.getNumeroCuenta() + " ya no está activa");
        }
        SaldoHandler.verificar(cuenta.getSaldoDisponible(), monto);

        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().subtract(monto));
        SaldoEntity saldo = cuenta.getSaldo();
        if (saldo != null) {
            saldo.setSaldoDisponible(saldo.getSaldoDisponible().subtract(monto));
            saldo.setSaldoRetenido(saldo.getSaldoRetenido().add(monto));
        }

        SendTxPayload payload = contexto.getPayload();
        TransaccionEntity transaccion = transaccionRepository.save(TransaccionEntity.builder()
                .usuario(cuenta.getUsuario())
                .cuenta(cuenta)
                .upc(payload.getUpc())
                .idServicio(payload.getIdServicio())
                .idProducto(payload.getIdProducto())
                .telefono(payload.getTelefono())
                .referencia(payload.getReferencia())
                .monto(monto)
                .horaLocal(payload.getHoraLocal())
                .unidad(payload.getUnidad())
                .estatus(EstatusTransaccionEnum.PENDIENTE)
                .payloadEnviado(contexto.getPayloadJson())
                .peticionCifrada(contexto.getPeticionCifrada())
                .fechaEnvio(OffsetDateTime.now(clock))
                .build());
        contexto.setTransaccionId(transaccion.getId());
        log.info("Transacción {} registrada como PENDIENTE; monto ${} retenido de la cuenta {}",
                payload.getUpc(), monto, cuenta.getNumeroCuenta());
    }

    @Transactional
    public TransaccionResponse registrarExitosa(Long transaccionId, GestoPagoTxResultado resultado, String respuestaXml) {
        TransaccionEntity tx = transaccionBloqueandoCuenta(transaccionId);
        SaldoEntity saldo = tx.getCuenta().getSaldo();
        if (saldo != null) {
            saldo.setSaldoRetenido(saldo.getSaldoRetenido().subtract(tx.getMonto()));
            saldo.setSaldoActual(saldo.getSaldoActual().subtract(tx.getMonto()));
        }
        tx.setEstatus(EstatusTransaccionEnum.EXITOSA);
        aplicarRespuesta(tx, resultado, respuestaXml);
        log.info("Transacción {} EXITOSA (autorización {})", tx.getUpc(), tx.getNumAutorizacion());
        return respuesta(tx, resultado);
    }

    @Transactional
    public TransaccionResponse registrarRechazada(Long transaccionId, GestoPagoTxResultado resultado, String respuestaXml) {
        TransaccionEntity tx = transaccionBloqueandoCuenta(transaccionId);
        liberarRetencion(tx);
        tx.setEstatus(EstatusTransaccionEnum.RECHAZADA);
        aplicarRespuesta(tx, resultado, respuestaXml);
        log.warn("Transacción {} RECHAZADA por GestoPago (código {}); monto liberado", tx.getUpc(), tx.getCodigoRespuesta());
        return respuesta(tx, resultado);
    }

    @Transactional
    public TransaccionResponse registrarPendiente(Long transaccionId, GestoPagoTxResultado resultado, String respuestaXml) {
        TransaccionEntity tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new TransaccionNoEncontradaException("No existe la transacción " + transaccionId));
        tx.setEstatus(EstatusTransaccionEnum.PENDIENTE_CONFIRMACION);
        aplicarRespuesta(tx, resultado, respuestaXml);
        log.warn("Transacción {} PENDIENTE_CONFIRMACION; el monto permanece retenido", tx.getUpc());
        return respuesta(tx, resultado);
    }

    @Transactional(readOnly = true)
    public TransaccionEntity buscarPorUpc(String upc) {
        TransaccionEntity tx = transaccionRepository.findByUpc(upc)
                .orElseThrow(() -> new TransaccionNoEncontradaException("No existe una transacción con el UPC " + upc));
        // Inicializa las relaciones que se consultan fuera de la transacción
        tx.getUsuario().getId();
        tx.getCuenta().getNumeroCuenta();
        return tx;
    }

    private TransaccionEntity transaccionBloqueandoCuenta(Long transaccionId) {
        TransaccionEntity tx = transaccionRepository.findById(transaccionId)
                .orElseThrow(() -> new TransaccionNoEncontradaException("No existe la transacción " + transaccionId));
        // getId() no inicializa el proxy: la cuenta se carga por primera vez ya bloqueada y con el saldo vigente
        cuentaRepository.findByIdParaActualizar(tx.getCuenta().getId());
        return tx;
    }

    private static void liberarRetencion(TransaccionEntity tx) {
        CuentaEntity cuenta = tx.getCuenta();
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(tx.getMonto()));
        SaldoEntity saldo = cuenta.getSaldo();
        if (saldo != null) {
            saldo.setSaldoDisponible(saldo.getSaldoDisponible().add(tx.getMonto()));
            saldo.setSaldoRetenido(saldo.getSaldoRetenido().subtract(tx.getMonto()));
        }
    }

    private static void aplicarRespuesta(TransaccionEntity tx, GestoPagoTxResultado resultado, String respuestaXml) {
        tx.setRespuestaGestoPago(respuestaXml);
        if (resultado == null) {
            return;
        }
        tx.setCodigoRespuesta(resultado.getCodigo());
        tx.setMensajeRespuesta(recortar(resultado.getTexto(), 600));
        if (resultado.getIdTx() != null && resultado.getIdTx() > 0) {
            tx.setIdTxGestoPago(resultado.getIdTx());
        }
        if (resultado.getNumAutorizacion() != null && !resultado.getNumAutorizacion().startsWith("-")) {
            tx.setNumAutorizacion(recortar(resultado.getNumAutorizacion(), 60));
        }
        if (resultado.getComision() != null) {
            tx.setComision(resultado.getComision().setScale(2, RoundingMode.HALF_UP));
        }
    }

    private static TransaccionResponse respuesta(TransaccionEntity tx, GestoPagoTxResultado resultado) {
        return TransaccionResponse.builder()
                .upc(tx.getUpc())
                .estatus(tx.getEstatus())
                .numCuenta(tx.getCuenta().getNumeroCuenta())
                .monto(tx.getMonto())
                .saldoDisponible(tx.getCuenta().getSaldoDisponible())
                .idTransaccionProveedor(tx.getIdTxGestoPago())
                .numeroAutorizacion(tx.getNumAutorizacion())
                .codigoProveedor(tx.getCodigoRespuesta())
                .mensajeProveedor(tx.getMensajeRespuesta())
                .fechaProveedor(resultado != null ? resultado.getFecha() : null)
                .referencia(resultado != null && resultado.getReferencia() != null ? resultado.getReferencia() : null)
                .pin(resultado != null ? resultado.getPin() : null)
                .build();
    }

    private static String recortar(String valor, int maximo) {
        return valor == null || valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
