package com.proyecto.servicios.security;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.OptionalLong;

/**
 * Códigos de un solo uso basados en tiempo (TOTP, RFC 6238) compatibles con Google Authenticator,
 * Microsoft Authenticator, Authy, etc.: HMAC-SHA1, intervalos de 30 segundos y 6 dígitos.
 *
 * <p>Se acepta el intervalo actual y el inmediato anterior o siguiente (tolerancia de reloj de ±30 s), y nunca
 * un intervalo igual o anterior al último ya utilizado, de modo que un mismo código no puede reutilizarse.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
public class TotpGenerador {

    private static final String ALFABETO_BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int BYTES_SECRETO = 20;
    private static final int SEGUNDOS_POR_PASO = 30;
    private static final int DIGITOS = 6;
    private static final int TOLERANCIA_PASOS = 1;

    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un secreto aleatorio de 160 bits codificado en Base32 (formato que leen las apps de autenticación).
     */
    public String generarSecreto() {
        byte[] bytes = new byte[BYTES_SECRETO];
        random.nextBytes(bytes);
        return base32(bytes);
    }

    /**
     * URI {@code otpauth://} para dar de alta la cuenta en la app de autenticación (se puede mostrar como QR).
     */
    public String uriProvisionamiento(String emisor, String cuenta, String secreto) {
        String etiqueta = codificar(emisor) + ":" + codificar(cuenta);
        return "otpauth://totp/" + etiqueta + "?secret=" + secreto + "&issuer=" + codificar(emisor)
                + "&algorithm=SHA1&digits=" + DIGITOS + "&period=" + SEGUNDOS_POR_PASO;
    }

    /**
     * Verifica un código.
     *
     * @param ultimoPasoUsado último intervalo aceptado para este secreto ({@code null} si nunca se usó).
     * @return el intervalo que coincidió, o vacío si el código es incorrecto, expiró o ya fue usado.
     */
    public OptionalLong verificar(String secreto, String codigo, Long ultimoPasoUsado, Instant ahora) {
        if (secreto == null || codigo == null || !codigo.matches("\\d{" + DIGITOS + "}")) {
            return OptionalLong.empty();
        }
        byte[] llave = desdeBase32(secreto);
        long pasoActual = ahora.getEpochSecond() / SEGUNDOS_POR_PASO;
        for (long paso = pasoActual - TOLERANCIA_PASOS; paso <= pasoActual + TOLERANCIA_PASOS; paso++) {
            if (ultimoPasoUsado != null && paso <= ultimoPasoUsado) {
                continue;
            }
            if (MessageDigest.isEqual(calcular(llave, paso).getBytes(StandardCharsets.US_ASCII),
                    codigo.getBytes(StandardCharsets.US_ASCII))) {
                return OptionalLong.of(paso);
            }
        }
        return OptionalLong.empty();
    }

    /**
     * Código vigente para un instante dado (usado en pruebas).
     */
    public String codigoEn(String secreto, Instant instante) {
        return calcular(desdeBase32(secreto), instante.getEpochSecond() / SEGUNDOS_POR_PASO);
    }

    private static String calcular(byte[] llave, long paso) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(llave, "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(paso).array());
            int desplazamiento = hash[hash.length - 1] & 0x0F;
            int binario = ((hash[desplazamiento] & 0x7F) << 24)
                    | ((hash[desplazamiento + 1] & 0xFF) << 16)
                    | ((hash[desplazamiento + 2] & 0xFF) << 8)
                    | (hash[desplazamiento + 3] & 0xFF);
            return String.format("%0" + DIGITOS + "d", binario % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA1 no disponible", e);
        }
    }

    private static String base32(byte[] datos) {
        StringBuilder salida = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xFF);
            bits += 8;
            while (bits >= 5) {
                salida.append(ALFABETO_BASE32.charAt((buffer >> (bits - 5)) & 0x1F));
                bits -= 5;
            }
        }
        if (bits > 0) {
            salida.append(ALFABETO_BASE32.charAt((buffer << (5 - bits)) & 0x1F));
        }
        return salida.toString();
    }

    private static byte[] desdeBase32(String texto) {
        String limpio = texto.replace("=", "").replace(" ", "").toUpperCase();
        ByteBuffer salida = ByteBuffer.allocate(limpio.length() * 5 / 8);
        int buffer = 0;
        int bits = 0;
        for (char c : limpio.toCharArray()) {
            int valor = ALFABETO_BASE32.indexOf(c);
            if (valor < 0) {
                throw new IllegalArgumentException("Secreto TOTP con caracteres no válidos");
            }
            buffer = (buffer << 5) | valor;
            bits += 5;
            if (bits >= 8) {
                salida.put((byte) ((buffer >> (bits - 8)) & 0xFF));
                bits -= 8;
            }
        }
        return salida.array();
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
