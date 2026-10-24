package com.proyecto.servicios.handler.transaccion;

/**
 * Un nodo del flujo de transacción. Si el paso no puede completarse lanza su excepción de negocio, lo que detiene
 * la cadena; {@code GlobalExceptionHandler} la convierte en la respuesta HTTP correspondiente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface PasoTransaccionHandler {

    void manejar(ContextoTransaccion contexto);
}
