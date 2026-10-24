package com.proyecto.servicios.security.gestopago;

import com.proyecto.servicios.exception.CifradoGestoPagoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * Cifrado del payload que exige GestoPago, fiel al "Encryption Snippet" de Java de su documentación:
 * AES-256 en modo CBC con relleno PKCS5, llave e IV entregados en Base64 y resultado codificado en Base64.
 *
 * <p>A diferencia del snippet (que imprime el error y devuelve {@code null}), aquí cualquier falla lanza
 * {@link CifradoGestoPagoException} para que el flujo se detenga antes de enviar datos a GestoPago.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
public class GestoPagoCifrador {

    private static final String TRANSFORMACION = "AES/CBC/PKCS5Padding";
    private static final int BYTES_LLAVE_AES_256 = 32;
    private static final int BYTES_IV = 16;

    private final SecretKey llave;
    private final IvParameterSpec iv;

    public GestoPagoCifrador(@Value("${gestopago.cifrado.secret-key}") String secretKeyBase64,
                             @Value("${gestopago.cifrado.iv}") String ivBase64) {
        byte[] llaveBytes = decodificar(secretKeyBase64, "gestopago.cifrado.secret-key");
        byte[] ivBytes = decodificar(ivBase64, "gestopago.cifrado.iv");
        if (llaveBytes.length != BYTES_LLAVE_AES_256) {
            throw new IllegalStateException("gestopago.cifrado.secret-key debe ser una llave AES-256 de 32 bytes en Base64");
        }
        if (ivBytes.length != BYTES_IV) {
            throw new IllegalStateException("gestopago.cifrado.iv debe ser un IV de 16 bytes en Base64");
        }
        this.llave = new SecretKeySpec(llaveBytes, 0, llaveBytes.length, "AES");
        this.iv = new IvParameterSpec(ivBytes);
    }

    /**
     * Cifra el JSON del payload; el resultado es el valor del parámetro {@code signed}.
     */
    public String cifrar(String textoPlano) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, llave, iv);
            return Base64.getEncoder().encodeToString(cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException | RuntimeException e) {
            log.error("No fue posible cifrar la petición para GestoPago: {}", e.getClass().getSimpleName());
            throw new CifradoGestoPagoException("No fue posible cifrar la petición para el procesador de pagos", e);
        }
    }

    /**
     * Descifra un valor producido por {@link #cifrar(String)} (o por GestoPago con la misma llave).
     */
    public String descifrar(String textoCifrado) {
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, llave, iv);
            return new String(cipher.doFinal(Base64.getDecoder().decode(textoCifrado)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | RuntimeException e) {
            log.error("No fue posible descifrar el contenido de GestoPago: {}", e.getClass().getSimpleName());
            throw new CifradoGestoPagoException("No fue posible descifrar el contenido del procesador de pagos", e);
        }
    }

    private static byte[] decodificar(String base64, String propiedad) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalStateException(propiedad + " es obligatorio");
        }
        try {
            return Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(propiedad + " no es un valor Base64 válido", e);
        }
    }
}
