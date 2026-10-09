package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;

import java.util.List;

/**
 * Interfaz de servicio para operaciones sobre cuentas bancarias asociadas a clientes.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface CuentaService {

    /**
     * Consulta el detalle de una cuenta bancaria por su número de cuenta de 16 dígitos.
     *
     * @param numeroCuenta número de cuenta único de 16 dígitos.
     * @return respuesta con los detalles de la cuenta.
     */
    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta);

    /**
     * Consulta todas las cuentas bancarias que se encuentran en estado ACTIVA.
     *
     * @return lista de respuestas de cuentas activas.
     */
    List<CuentaResponse> consultarCuentasActivas();

    /**
     * Consulta el saldo disponible y contable de una cuenta por su número de cuenta.
     *
     * @param numeroCuenta número de cuenta de 16 dígitos.
     * @return respuesta detallada del saldo.
     */
    SaldoResponse consultarSaldoPorNumeroCuenta(String numeroCuenta);

    /**
     * Genera un número de cuenta único de 16 dígitos asegurando no colisión en la base de datos.
     *
     * @return número de cuenta generado de 16 dígitos numéricos.
     */
    String generarNumeroCuentaUnico();
}
