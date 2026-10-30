package com.proyecto.servicios.security.tls;

import org.springframework.boot.ssl.SslBundle;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link SSLSocketFactory} que solo habilita las versiones de TLS permitidas (por ejemplo TLSv1.3 y TLSv1.2).
 *
 * <p>Un {@code SSLContext} de Java acepta por defecto versiones anteriores si el servidor las negocia; esta clase
 * las deshabilita en cada socket para que la conexión falle antes que degradarse a un protocolo inseguro.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class ProtocolosTlsSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegado;
    private final String[] protocolos;

    public ProtocolosTlsSocketFactory(SSLSocketFactory delegado, Set<String> protocolosPermitidos) {
        if (protocolosPermitidos == null || protocolosPermitidos.isEmpty()) {
            throw new IllegalArgumentException("Se debe indicar al menos una versión de TLS permitida");
        }
        this.delegado = delegado;
        this.protocolos = protocolosPermitidos.toArray(String[]::new);
    }

    /**
     * Crea el socket factory a partir de un SSL bundle de Spring: su truststore y sus {@code enabled-protocols}.
     */
    public static ProtocolosTlsSocketFactory desde(SslBundle bundle) {
        String[] protocolos = bundle.getOptions().getEnabledProtocols();
        if (protocolos == null) {
            throw new IllegalStateException("El SSL bundle debe definir options.enabled-protocols (por ejemplo TLSv1.3,TLSv1.2)");
        }
        return new ProtocolosTlsSocketFactory(bundle.createSslContext().getSocketFactory(),
                new LinkedHashSet<>(Arrays.asList(protocolos)));
    }

    public String[] getProtocolos() {
        return protocolos.clone();
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegado.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegado.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return restringir(delegado.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(Socket s, InputStream consumed, boolean autoClose) throws IOException {
        return restringir(delegado.createSocket(s, consumed, autoClose));
    }

    @Override
    public Socket createSocket() throws IOException {
        return restringir(delegado.createSocket());
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return restringir(delegado.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return restringir(delegado.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return restringir(delegado.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
        return restringir(delegado.createSocket(address, port, localAddress, localPort));
    }

    private Socket restringir(Socket socket) {
        if (socket instanceof SSLSocket ssl) {
            Set<String> soportados = Set.of(ssl.getSupportedProtocols());
            String[] habilitados = Arrays.stream(protocolos).filter(soportados::contains).toArray(String[]::new);
            if (habilitados.length == 0) {
                throw new IllegalStateException("La JVM no soporta ninguna de las versiones de TLS permitidas: "
                        + String.join(", ", protocolos));
            }
            ssl.setEnabledProtocols(habilitados);
        }
        return socket;
    }
}
