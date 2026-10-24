package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Estado que recorre los handlers del flujo de transacción. Cada handler lee lo que necesita y deja su
 * resultado para el siguiente (cuenta validada, payload, petición cifrada, token, respuesta de GestoPago...).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
@Setter
public class ContextoTransaccion {

    private final TransaccionRequest request;
    private final UsuarioAutenticado usuario;

    // Nodo «Cliente Activo»
    private Long clienteId;
    private Long cuentaId;
    private BigDecimal saldoDisponibleInicial;

    // Nodo «Sen Tx» (armado del payload)
    private ProductoCatalogoEntity producto;
    private SendTxPayload payload;
    private String payloadJson;

    // Nodo «Cifrar Petición»
    private String random;
    private String peticionCifrada;

    // Nodo «Obtener Token»
    private String token;

    // Nodo «Sen Tx Cifrado + Token» y registro
    private Long transaccionId;
    private String respuestaXml;
    private GestoPagoTxResultado resultado;
    private TransaccionResponse respuesta;

    public ContextoTransaccion(TransaccionRequest request, UsuarioAutenticado usuario) {
        this.request = request;
        this.usuario = usuario;
    }
}
