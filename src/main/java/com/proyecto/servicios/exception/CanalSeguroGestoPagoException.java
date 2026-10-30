package com.proyecto.servicios.exception;

/**
 * Excepción lanzada cuando falla el handshake TLS con GestoPago (certificado no confiable, nombre de host distinto o
 * protocolo no permitido). Como el handshake ocurre antes de enviar la petición, GestoPago nunca la recibió.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class CanalSeguroGestoPagoException extends RuntimeException {

    public CanalSeguroGestoPagoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
