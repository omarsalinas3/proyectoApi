package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.exception.SaldoInsuficienteException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Nodo «Saldo»: el saldo disponible debe cubrir el monto. La comprobación se repite de forma atómica (con la fila
 * de la cuenta bloqueada) al retener el monto, para cubrir operaciones simultáneas.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
public class SaldoHandler implements PasoTransaccionHandler {

    @Override
    public void manejar(ContextoTransaccion contexto) {
        verificar(contexto.getSaldoDisponibleInicial(), contexto.getRequest().getMonto());
    }

    static void verificar(BigDecimal saldoDisponible, BigDecimal monto) {
        BigDecimal disponible = saldoDisponible != null ? saldoDisponible : BigDecimal.ZERO;
        if (disponible.compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(String.format(
                    "Saldo insuficiente: disponible $%s, monto solicitado $%s",
                    disponible.setScale(2, RoundingMode.HALF_UP), monto.setScale(2, RoundingMode.HALF_UP)));
        }
    }
}
