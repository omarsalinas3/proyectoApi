package com.proyecto.servicios.security.tls;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.net.ssl.SNIHostName;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Revisa al arrancar que el canal TLS hacia GestoPago funcione con el truststore propio y avisa si el certificado del
 * servidor está por vencer.
 *
 * <p>Solo hace el handshake: no envía ninguna petición HTTP, así que no consume los límites de uso de GestoPago
 * (token una vez al día, catálogo tres veces al día). Corre en un hilo aparte para no retrasar el arranque, y un fallo
 * solo se registra en el log: la aplicación sigue atendiendo el resto de la API.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "gestopago.tls.verificar-al-arrancar", havingValue = "true", matchIfMissing = true)
public class GestoPagoTlsVerificador {

    private final ProtocolosTlsSocketFactory socketFactory;
    private final URI url;
    private final long diasAviso;
    private final int timeoutMs;
    private final Clock clock;

    public GestoPagoTlsVerificador(ProtocolosTlsSocketFactory socketFactory,
                                   @Value("${gestopago.transaccion.url}") String url,
                                   @Value("${gestopago.tls.dias-aviso-vencimiento:15}") long diasAviso,
                                   @Value("${gestopago.transaccion.connect-timeout-ms:5000}") int timeoutMs,
                                   Clock clock) {
        this.socketFactory = socketFactory;
        this.url = URI.create(url);
        this.diasAviso = diasAviso;
        this.timeoutMs = timeoutMs;
        this.clock = clock;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        Thread.ofVirtual().name("verificacion-tls-gestopago").start(() -> {
            try {
                verificar();
            } catch (IOException e) {
                log.error("Canal TLS con GestoPago NO verificado ({}): las peticiones al procesador de pagos fallarán. "
                        + "Revise el certificado del servidor o el PEM de GESTOPAGO_TLS_CA", e.getMessage());
            }
        });
    }

    /**
     * Hace el handshake con el host de GestoPago y devuelve los datos del certificado del servidor.
     *
     * @throws IOException si el handshake falla (CA no confiable, nombre de host distinto, protocolo no permitido)
     */
    public ResultadoVerificacionTls verificar() throws IOException {
        String host = url.getHost();
        int puerto = url.getPort() > 0 ? url.getPort() : 443;
        try (SSLSocket socket = (SSLSocket) socketFactory.createSocket()) {
            SSLParameters parametros = socket.getSSLParameters();
            parametros.setServerNames(List.of(new SNIHostName(host)));
            // Misma verificación de nombre de host que hace HttpsURLConnection
            parametros.setEndpointIdentificationAlgorithm("HTTPS");
            socket.setSSLParameters(parametros);
            socket.setSoTimeout(timeoutMs);
            socket.connect(new InetSocketAddress(host, puerto), timeoutMs);
            socket.startHandshake();

            SSLSession sesion = socket.getSession();
            X509Certificate certificado = (X509Certificate) sesion.getPeerCertificates()[0];
            long diasRestantes = Duration.between(Instant.now(clock), certificado.getNotAfter().toInstant()).toDays();
            ResultadoVerificacionTls resultado = new ResultadoVerificacionTls(host, sesion.getProtocol(),
                    sesion.getCipherSuite(), certificado.getSubjectX500Principal().getName(),
                    certificado.getIssuerX500Principal().getName(), certificado.getNotAfter().toInstant(), diasRestantes);

            log.info("Canal TLS con {} verificado: {} {}, certificado de '{}' emitido por '{}', vence en {} días",
                    host, resultado.protocolo(), resultado.cifrado(), resultado.sujeto(), resultado.emisor(), diasRestantes);
            if (diasRestantes <= diasAviso) {
                log.warn("El certificado TLS de {} vence en {} días ({}). Al renovarse se seguirá aceptando mientras lo "
                        + "emita una CA del truststore; si cambia de CA, actualice GESTOPAGO_TLS_CA", host, diasRestantes,
                        resultado.vence());
            }
            return resultado;
        }
    }

    /** Datos del canal y del certificado del servidor obtenidos en la verificación. */
    public record ResultadoVerificacionTls(String host, String protocolo, String cifrado, String sujeto, String emisor,
                                           Instant vence, long diasRestantes) {
    }
}
