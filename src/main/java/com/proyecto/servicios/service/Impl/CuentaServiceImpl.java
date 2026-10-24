package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.exception.ClienteInactivoException;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.model.common.PaginaResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.CuentaSpecifications;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Implementación del servicio de gestión de cuentas bancarias.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<CuentaResponse> buscar(CuentaBusquedaRequest filtros) {
        log.info("Buscando cuentas con filtros: clienteId={}, estatus={}, pagina={}, tamanio={}",
                filtros.getClienteId(), filtros.getEstatus(), filtros.getPagina(), filtros.getTamanio());
        PageRequest pagina = PageRequest.of(filtros.getPagina(), filtros.getTamanio(), Sort.by(Sort.Direction.ASC, "id"));
        Page<CuentaEntity> resultado = cuentaRepository.findAll(CuentaSpecifications.conFiltros(filtros), pagina);
        return PaginaResponse.de(resultado, clienteMapper::toCuentaResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldoPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando saldo para la cuenta: {}", numeroCuenta);
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número: " + numeroCuenta));

        SaldoEntity saldo = saldoRepository.findByCuentaId(cuenta.getId())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró registro de saldos para la cuenta: " + numeroCuenta));

        return clienteMapper.toSaldoResponse(saldo);
    }

    @Override
    public String generarNumeroCuentaUnico() {
        String numeroCuenta;
        boolean existe;
        do {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                sb.append(ThreadLocalRandom.current().nextInt(10));
            }
            numeroCuenta = sb.toString();
            existe = cuentaRepository.existsByNumeroCuenta(numeroCuenta);
        } while (existe);

        return numeroCuenta;
    }

    @Override
    @Transactional
    public SaldoResponse depositar(String numeroCuenta, BigDecimal monto) {
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuentaParaActualizar(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número: " + numeroCuenta));
        if (!"ACTIVA".equals(cuenta.getEstatus())) {
            throw new ClienteInactivoException("La cuenta " + numeroCuenta + " está en estatus " + cuenta.getEstatus()
                    + " y no puede recibir depósitos");
        }
        BigDecimal abono = monto.setScale(2, RoundingMode.HALF_UP);
        cuenta.setSaldoDisponible(cuenta.getSaldoDisponible().add(abono));
        SaldoEntity saldo = saldoRepository.findByCuentaId(cuenta.getId())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró registro de saldos para la cuenta: " + numeroCuenta));
        saldo.setSaldoActual(saldo.getSaldoActual().add(abono));
        saldo.setSaldoDisponible(saldo.getSaldoDisponible().add(abono));
        log.info("Depósito de ${} a la cuenta {}", abono, numeroCuenta);
        return clienteMapper.toSaldoResponse(saldo);
    }
}
