package com.proyecto.servicios.security;

import com.proyecto.servicios.enums.RolEnum;

/**
 * Identidad del usuario autenticado extraída del token JWT.
 *
 * @param clienteId identificador del cliente dueño de la sesión ({@code null} para el rol EJECUTIVO).
 * @param username  usuario o correo con el que se inició sesión.
 * @param rol       rol de acceso.
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public record UsuarioAutenticado(Long clienteId, String username, RolEnum rol) {

    public boolean esEjecutivo() {
        return rol == RolEnum.EJECUTIVO;
    }
}
