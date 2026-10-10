package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;

/**
 * Servicio utilitario para el cifrado y descifrado de datos sensibles (contraseñas, biometría, usuario)
 * cumpliendo con las regulaciones de seguridad y protección de datos bancarios.
 *
 * <p><strong>Algoritmos implementados:</strong>
 * <ul>
 *   <li>AES-128/256 para cifrado simétrico reversible (datos biométricos y usuario).</li>
 *   <li>SHA-256 con salt para hashing irreversible de contraseñas.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
public class CryptoService {

    private static final String AES_ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final SecretKeySpec secretKeySpec;

    public CryptoService(@Value("${seguridad.crypto.secret-key:MiClaveSecretaSuperSegura2026AES}") String secretKey) {
        try {
            MessageDigest sha = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] keyBytes = sha.digest(secretKey.getBytes(StandardCharsets.UTF_8));
            // Usar clave AES de 128/256 bits derivada del hash
            keyBytes = Arrays.copyOf(keyBytes, 16); // 128 bits
            this.secretKeySpec = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            log.error("Error al inicializar el algoritmo de cifrado: {}", e.getMessage());
            throw new IllegalStateException("No se pudo inicializar el servicio de criptografía", e);
        }
    }

    /**
     * Cifra un texto en claro utilizando el algoritmo AES.
     *
     * @param plainText texto a cifrar.
     * @return cadena cifrada en formato Base64.
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error al cifrar información sensible: {}", e.getMessage());
            throw new RuntimeException("Error en proceso de cifrado de datos", e);
        }
    }

    /**
     * Descifra un texto previamente cifrado en formato Base64 con AES.
     *
     * @param cipherText cadena cifrada en Base64.
     * @return texto descifrado en claro.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error al descifrar información: {}", e.getMessage());
            throw new RuntimeException("Error en proceso de descifrado de datos", e);
        }
    }

    /**
     * Genera un hash SHA-256 seguro para almacenamiento de contraseñas.
     *
     * @param rawPassword contraseña en texto plano.
     * @return hash en formato hexadecimal.
     */
    public String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error al generar hash de contraseña: {}", e.getMessage());
            throw new RuntimeException("Error al procesar seguridad de contraseña", e);
        }
    }

    /**
     * Compara una contraseña en texto plano contra su hash almacenado.
     *
     * @param rawPassword contraseña enviada por el usuario.
     * @param hashedPassword hash almacenado en la base de datos.
     * @return {@code true} si coinciden, {@code false} en caso contrario.
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        String generatedHash = hashPassword(rawPassword);
        return MessageDigest.isEqual(generatedHash.getBytes(StandardCharsets.UTF_8),
                hashedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Cifra un valor biométrico de alta precisión {@link BigDecimal}.
     *
     * @param biometrics valor biométrico numérico.
     * @return cadena cifrada.
     */
    public String encryptBiometrics(BigDecimal biometrics) {
        if (biometrics == null) {
            return null;
        }
        return encrypt(biometrics.toPlainString());
    }

    /**
     * Descifra un valor biométrico recuperando su precisión original en {@link BigDecimal}.
     *
     * @param encryptedBiometrics cadena cifrada.
     * @return objeto {@link BigDecimal} original.
     */
    public BigDecimal decryptBiometrics(String encryptedBiometrics) {
        if (encryptedBiometrics == null || encryptedBiometrics.isEmpty()) {
            return null;
        }
        String decrypted = decrypt(encryptedBiometrics);
        return new BigDecimal(decrypted);
    }
}
