package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando una sesión se encuentra inactiva o expirada por haber superado el tiempo límite dinámico.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class SesionExpiradaException extends RuntimeException {

    public SesionExpiradaException(String mensaje) {
        super(mensaje);
    }
}
