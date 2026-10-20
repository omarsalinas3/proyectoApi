package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el usuario autenticado intenta acceder a un recurso (cliente, cuenta, sesión) que no le pertenece o que su rol no permite consultar.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class AccesoDenegadoException extends RuntimeException {

    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
