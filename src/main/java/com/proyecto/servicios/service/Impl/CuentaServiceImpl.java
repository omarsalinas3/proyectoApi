package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.repositorys.cliente.CuentaRepository;
import com.proyecto.servicios.repositorys.cliente.SaldoRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta bancaria por número: {}", numeroCuenta);
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número: " + numeroCuenta));

        CuentaResponse response = clienteMapper.toCuentaResponse(cuenta);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cuenta bancaria consultada exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponse> consultarCuentasActivas() {
        log.info("Consultando listado de cuentas activas en el sistema");
        List<CuentaEntity> cuentas = cuentaRepository.findByEstatus("ACTIVA");
        return cuentas.stream()
                .map(c -> {
                    CuentaResponse response = clienteMapper.toCuentaResponse(c);
                    response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
                    response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
                    response.setMensaje("Cuenta activa recuperada");
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SaldoResponse consultarSaldoPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando saldo para la cuenta: {}", numeroCuenta);
        CuentaEntity cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró la cuenta bancaria con número: " + numeroCuenta));

        SaldoEntity saldo = saldoRepository.findByCuentaId(cuenta.getId())
                .orElseThrow(() -> new CuentaNoEncontradaException("No se encontró registro de saldos para la cuenta: " + numeroCuenta));

        SaldoResponse response = clienteMapper.toSaldoResponse(saldo);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Saldo consultado exitosamente");
        return response;
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
}
