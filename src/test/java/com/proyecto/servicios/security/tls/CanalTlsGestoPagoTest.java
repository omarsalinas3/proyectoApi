package com.proyecto.servicios.security.tls;

import feign.Client;
import feign.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslOptions;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del canal TLS hacia GestoPago sin salir a internet: se levanta un servidor TLS local con un certificado
 * autofirmado (generado con keytool) y se comprueba que el canal configurado lo rechaza, que la restricción de
 * protocolos funciona y que el mismo truststore de la aplicación es el que se carga.
 */
@DisplayName("Pruebas - Canal TLS hacia GestoPago")
class CanalTlsGestoPagoTest {

    private static final char[] CLAVE = "clave-de-prueba".toCharArray();
    private static final String ALIAS = "servidor-prueba";

    @TempDir
    static Path directorio;

    private static KeyStore almacenServidor;
    private SSLServerSocket servidor;

    @BeforeAll
    static void generarCertificadoAutofirmado() throws Exception {
        Path p12 = directorio.resolve("servidor.p12");
        String keytool = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase().contains("win") ? "keytool.exe" : "keytool").toString();
        Process proceso = new ProcessBuilder(keytool, "-genkeypair", "-alias", ALIAS, "-keyalg", "RSA", "-keysize", "2048",
                "-validity", "2", "-dname", "CN=localhost", "-ext", "SAN=dns:localhost,ip:127.0.0.1",
                "-storetype", "PKCS12", "-keystore", p12.toString(), "-storepass", new String(CLAVE), "-keypass", new String(CLAVE))
                .redirectErrorStream(true).start();
        assertTrue(proceso.waitFor(60, TimeUnit.SECONDS), "keytool no terminó");
        assertEquals(0, proceso.exitValue(), () -> salida(proceso));
        almacenServidor = KeyStore.getInstance("PKCS12");
        try (InputStream in = Files.newInputStream(p12)) {
            almacenServidor.load(in, CLAVE);
        }
    }

    @AfterEach
    void detenerServidor() throws IOException {
        if (servidor != null) {
            servidor.close();
        }
    }

    @Test
    @DisplayName("El PEM de la aplicación contiene solo las raíces de DigiCert que emiten el certificado de GestoPago")
    void truststoreContieneRaicesDigiCert() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/tls/gestopago-ca.pem")) {
            assertNotNull(in, "Falta src/main/resources/tls/gestopago-ca.pem");
            Collection<? extends Certificate> certificados = CertificateFactory.getInstance("X.509").generateCertificates(in);
            List<String> sujetos = certificados.stream()
                    .map(c -> ((X509Certificate) c).getSubjectX500Principal().getName()).toList();
            assertEquals(3, sujetos.size());
            assertTrue(sujetos.stream().allMatch(s -> s.contains("O=DigiCert Inc")), sujetos::toString);
            assertTrue(sujetos.stream().anyMatch(s -> s.contains("CN=DigiCert Global Root G2")), sujetos::toString);
        }
    }

    @Test
    @DisplayName("Rechaza un servidor cuyo certificado no lo emitió una CA del truststore (aunque el host sea correcto)")
    void rechazaCertificadoNoConfiable() throws Exception {
        int puerto = iniciarServidor("TLSv1.3", "TLSv1.2");
        ProtocolosTlsSocketFactory canal = canalDeLaAplicacion();

        try (SSLSocket socket = (SSLSocket) canal.createSocket("localhost", puerto)) {
            assertThrows(SSLHandshakeException.class, socket::startHandshake);
        }
    }

    @Test
    @DisplayName("Feign con el canal de la aplicación falla con SSLHandshakeException antes de enviar la petición")
    void feignFallaEnElHandshake() throws Exception {
        int puerto = iniciarServidor("TLSv1.3", "TLSv1.2");
        Client cliente = new Client.Default(canalDeLaAplicacion(), null);
        Request peticion = Request.create(Request.HttpMethod.POST, "https://localhost:" + puerto + "/sistema/service/sendTx.do",
                Map.of(), "random=1&signed=x".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8, null);

        IOException ex = assertThrows(IOException.class, () -> cliente.execute(peticion, new Request.Options()));

        assertInstanceOf(SSLHandshakeException.class, ex);
    }

    @Test
    @DisplayName("Con un truststore que sí confía en el servidor, el handshake se completa con TLS 1.3")
    void aceptaServidorConfiable() throws Exception {
        int puerto = iniciarServidor("TLSv1.3", "TLSv1.2");
        ProtocolosTlsSocketFactory canal = canalQueConfiaEnElServidor(Set.of("TLSv1.3", "TLSv1.2"));

        try (SSLSocket socket = (SSLSocket) canal.createSocket("localhost", puerto)) {
            socket.startHandshake();
            assertEquals("TLSv1.3", socket.getSession().getProtocol());
        }
    }

    @Test
    @DisplayName("No se degrada: si el servidor solo ofrece TLS 1.2 y solo se permite 1.3, la conexión falla")
    void noAceptaProtocoloNoPermitido() throws Exception {
        int puerto = iniciarServidor("TLSv1.2");
        ProtocolosTlsSocketFactory canal = canalQueConfiaEnElServidor(Set.of("TLSv1.3"));

        try (SSLSocket socket = (SSLSocket) canal.createSocket("localhost", puerto)) {
            assertArrayEquals(new String[]{"TLSv1.3"}, socket.getEnabledProtocols());
            assertThrows(SSLHandshakeException.class, socket::startHandshake);
        }
    }

    @Test
    @DisplayName("Rechaza una configuración sin versiones de TLS")
    void rechazaConfiguracionVacia() throws Exception {
        assertThrows(IllegalArgumentException.class,
                () -> new ProtocolosTlsSocketFactory(SSLContext.getDefault().getSocketFactory(), Set.of()));
    }

    /** Mismo armado que {@code GestoPagoTlsConfig}: bundle PEM con las raíces de DigiCert y TLS 1.3/1.2. */
    private static ProtocolosTlsSocketFactory canalDeLaAplicacion() {
        SslBundle bundle = SslBundle.of(
                new PemSslStoreBundle(null, PemSslStoreDetails.forCertificate("classpath:tls/gestopago-ca.pem")),
                null, SslOptions.of(null, Set.of("TLSv1.3", "TLSv1.2")));
        return ProtocolosTlsSocketFactory.desde(bundle);
    }

    private static ProtocolosTlsSocketFactory canalQueConfiaEnElServidor(Set<String> protocolos) throws Exception {
        KeyStore confianza = KeyStore.getInstance("PKCS12");
        confianza.load(null, null);
        confianza.setCertificateEntry("servidor", almacenServidor.getCertificate(ALIAS));
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(confianza);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(null, tmf.getTrustManagers(), null);
        return new ProtocolosTlsSocketFactory(contexto.getSocketFactory(), protocolos);
    }

    private int iniciarServidor(String... protocolos) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(almacenServidor, CLAVE);
        SSLContext contexto = SSLContext.getInstance("TLS");
        contexto.init(kmf.getKeyManagers(), null, null);
        servidor = (SSLServerSocket) contexto.getServerSocketFactory().createServerSocket(0);
        servidor.setEnabledProtocols(protocolos);
        Thread.ofVirtual().start(() -> {
            while (!servidor.isClosed()) {
                try (SSLSocket conexion = (SSLSocket) servidor.accept()) {
                    conexion.startHandshake();
                } catch (IOException ignorada) {
                    // El cliente cierra o rechaza el handshake: es justo lo que se prueba
                }
            }
        });
        return servidor.getLocalPort();
    }

    private static String salida(Process proceso) {
        try {
            return new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
