package com.proyecto.servicios.config;

import com.proyecto.servicios.security.tls.ProtocolosTlsSocketFactory;
import feign.Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Canal TLS hacia GestoPago para todos los clientes Feign (token, catálogo, sendTx y confirmTx).
 *
 * <ul>
 *   <li>Solo confía en las CA del bundle {@code gestopago} ({@code tls/gestopago-ca.pem}, raíces de DigiCert), no en
 *   todo el almacén de la JVM: un certificado emitido por otra CA, aunque sea válido, se rechaza.</li>
 *   <li>Se fija la CA raíz y no el certificado del servidor, porque GestoPago lo renueva cada año y fijarlo rompería
 *   la integración en cada renovación.</li>
 *   <li>Solo TLSv1.3 y TLSv1.2 (configurable); nunca se degrada a versiones inseguras.</li>
 *   <li>Se conserva la verificación del nombre de host de Java (no se usa un HostnameVerifier permisivo).</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Configuration
public class GestoPagoTlsConfig {

    @Bean
    public ProtocolosTlsSocketFactory gestoPagoSocketFactory(SslBundles sslBundles,
                                                             @Value("${gestopago.tls.bundle:gestopago}") String nombreBundle) {
        ProtocolosTlsSocketFactory factory = ProtocolosTlsSocketFactory.desde(sslBundles.getBundle(nombreBundle));
        log.info("Canal TLS hacia GestoPago: bundle '{}', protocolos {}", nombreBundle, String.join(", ", factory.getProtocolos()));
        return factory;
    }

    /**
     * Cliente HTTP que usan todos los {@code @FeignClient} del proyecto. El {@code HostnameVerifier} nulo deja activa la
     * verificación estándar de Java.
     */
    @Bean
    public Client gestoPagoFeignClient(ProtocolosTlsSocketFactory gestoPagoSocketFactory) {
        return new Client.Default(gestoPagoSocketFactory, null);
    }
}
