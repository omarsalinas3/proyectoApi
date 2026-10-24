package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando el producto o servicio solicitado no existe en el catálogo o el monto no corresponde a su precio (nodo «Sen Tx»).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ProductoNoDisponibleException extends RuntimeException {

    public ProductoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
