package com.proyecto.servicios.handler.transaccion;

import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.model.transaccion.response.TransaccionResponse;
import com.proyecto.servicios.security.UsuarioAutenticado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Handler del flujo de pago con GestoPago ({@code sendTx}). Ejecuta los nodos del diagrama en orden y se detiene
 * en el primero que falle (cada uno lanza su propia excepción, procesada por {@code GlobalExceptionHandler}):
 *
 * <ol>
 *   <li>Cliente Activo → Saldo → 2FA: validaciones previas; si alguna falla no se envía nada a GestoPago.</li>
 *   <li>Sen Tx: armado del payload con las reglas y longitudes de la documentación.</li>
 *   <li>Cifrar Petición: AES-256-CBC.</li>
 *   <li>Obtener Token: token vigente (uno por día).</li>
 *   <li>Retención del monto y registro PENDIENTE (con la cuenta bloqueada).</li>
 *   <li>Sen Tx Cifrado + Token: envío a GestoPago.</li>
 *   <li>¡Pago Exitoso!: registro del resultado y cargo definitivo (o liberación / pendiente).</li>
 * </ol>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendTxHandler {

    private final ClienteActivoHandler clienteActivoHandler;
    private final SaldoHandler saldoHandler;
    private final DosFactoresHandler dosFactoresHandler;
    private final ArmadoPayloadHandler armadoPayloadHandler;
    private final CifradoPeticionHandler cifradoPeticionHandler;
    private final TokenHandler tokenHandler;
    private final RegistroTransaccion registroTransaccion;
    private final EnvioTransaccionHandler envioTransaccionHandler;
    private final ResultadoTransaccionHandler resultadoTransaccionHandler;

    public TransaccionResponse ejecutar(TransaccionRequest request, UsuarioAutenticado usuario) {
        ContextoTransaccion contexto = new ContextoTransaccion(request, usuario);

        // 1. Validaciones previas de negocio
        clienteActivoHandler.manejar(contexto);
        saldoHandler.manejar(contexto);
        dosFactoresHandler.manejar(contexto);

        // 2-4. Preparación de la petición
        armadoPayloadHandler.manejar(contexto);
        cifradoPeticionHandler.manejar(contexto);
        tokenHandler.manejar(contexto);

        // 5. Retención del monto y evidencia de la petición
        registroTransaccion.reservar(contexto);
        log.info("Enviando transacción {} (cliente {}, producto {}, monto ${})", contexto.getPayload().getUpc(),
                contexto.getClienteId(), request.getIdProducto(), request.getMonto());

        // 6-7. Envío a GestoPago y registro del resultado
        envioTransaccionHandler.manejar(contexto);
        resultadoTransaccionHandler.manejar(contexto);
        return contexto.getRespuesta();
    }
}
