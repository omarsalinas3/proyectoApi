package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.response.DosFactoresResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;

/**
 * Segundo factor de autenticación (TOTP) de los clientes.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface DosFactoresService {

    /**
     * Genera un secreto nuevo (aún inactivo) y devuelve los datos para darlo de alta en la app de autenticación.
     *
     * @throws com.proyecto.servicios.exception.ValidacionNegocioException si el 2FA ya está activo.
     */
    DosFactoresResponse iniciarActivacion(UsuarioAutenticado usuario);

    /**
     * Activa el 2FA tras comprobar un código generado con el secreto recién entregado.
     *
     * @throws com.proyecto.servicios.exception.DosFactoresInvalidoException si el código no es válido.
     */
    DosFactoresResponse confirmarActivacion(UsuarioAutenticado usuario, String codigo);

    /**
     * Verifica el código 2FA de una operación y lo marca como usado.
     *
     * @throws com.proyecto.servicios.exception.DosFactoresNoConfiguradoException si el cliente no activó el 2FA.
     * @throws com.proyecto.servicios.exception.DosFactoresInvalidoException si el código no es válido o ya se usó.
     */
    void validarCodigo(Long clienteId, String codigo);
}
