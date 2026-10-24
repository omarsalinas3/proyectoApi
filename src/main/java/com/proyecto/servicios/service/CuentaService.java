package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import com.proyecto.servicios.model.common.PaginaResponse;

/**
 * Interfaz de servicio para la consulta de Cuentas Bancarias y Saldos.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
public interface CuentaService {

    /**
     * Busca cuentas combinando los filtros informados (número de cuenta, titular y estatus).
     *
     * @param filtros criterios de búsqueda y paginación.
     * @return página de cuentas que cumplen los filtros.
     */
    PaginaResponse<CuentaResponse> buscar(CuentaBusquedaRequest filtros);

    /**
     * Consulta el saldo de una cuenta.
     *
     * @param numeroCuenta número de cuenta de 16 dígitos.
     * @return desglose de saldos.
     * @throws com.proyecto.servicios.exception.CuentaNoEncontradaException si la cuenta no existe.
     */
    SaldoResponse consultarSaldoPorNumeroCuenta(String numeroCuenta);

    /**
     * Genera un número de cuenta de 16 dígitos que no exista en la base de datos.
     *
     * @return número de cuenta único.
     */
    String generarNumeroCuentaUnico();

    /**
     * Abona saldo a una cuenta activa.
     *
     * @param numeroCuenta número de cuenta de 16 dígitos.
     * @param monto        monto a abonar (mayor a 0).
     * @return saldo actualizado.
     * @throws com.proyecto.servicios.exception.CuentaNoEncontradaException si la cuenta no existe.
     */
    SaldoResponse depositar(String numeroCuenta, java.math.BigDecimal monto);
}
