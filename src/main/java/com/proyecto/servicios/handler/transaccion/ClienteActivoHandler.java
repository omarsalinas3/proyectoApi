package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nodo «Cliente Activo»: la cuenta debe pertenecer al cliente autenticado, el cliente debe estar activo y la
 * cuenta en estatus ACTIVA.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteActivoHandler implements PasoTransaccionHandler {

    static final String ESTATUS_CUENTA_ACTIVA = "ACTIVA";

    private final CuentaRepository cuentaRepository;

    @Override
    @Transactional(readOnly = true)
    public void manejar(ContextoTransaccion contexto) {
        UsuarioAutenticado usuario = contexto.getUsuario();
        if (usuario.esEjecutivo()) {
            throw new AccesoDenegadoException("Las transacciones solo las puede realizar el titular de la cuenta");
        }
        String numCuenta = contexto.getRequest().getNumCuenta();
        // Cuenta inexistente y cuenta ajena responden igual para no revelar qué cuentas existen
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(numCuenta)
                .filter(c -> c.getUsuario() != null && c.getUsuario().getId().equals(usuario.clienteId()))
                .orElseThrow(() -> {
                    log.warn("El cliente {} intentó operar con una cuenta que no le pertenece", usuario.clienteId());
                    return new AccesoDenegadoException("La cuenta indicada no pertenece al cliente autenticado");
                });

        UsuarioEntity cliente = cuenta.getUsuario();
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException("El cliente está dado de baja y no puede realizar transacciones");
        }
        if (!ESTATUS_CUENTA_ACTIVA.equals(cuenta.getEstatus())) {
            throw new ClienteInactivoException("La cuenta " + numCuenta + " está en estatus " + cuenta.getEstatus()
                    + " y no puede operar");
        }

        contexto.setClienteId(cliente.getId());
        contexto.setCuentaId(cuenta.getId());
        contexto.setSaldoDisponibleInicial(cuenta.getSaldoDisponible());
    }
}
