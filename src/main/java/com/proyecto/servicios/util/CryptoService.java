package com.proyecto.servicios.util;

import lombok.extern.slf4j.Slf4j;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * Servicio utilitario para el cifrado y descifrado de datos sensibles (contraseñas, biometría, usuario)
 * cumpliendo con las regulaciones de seguridad y protección de datos bancarios.
 *
 * <p><strong>Algoritmos implementados:</strong>
 * <ul>
 *   <li>AES-256-GCM con IV aleatorio para cifrado reversible (usuario, secreto 2FA y biometría). El texto cifrado
 *   lleva el prefijo {@code v2:}. Cifrar dos veces el mismo valor da textos distintos, así que nunca se compara ni
 *   se busca por el texto cifrado: para buscar se usa {@link #hashBusqueda(String)}.</li>
 *   <li>Lectura de datos heredados en AES/ECB (sin prefijo), solo mientras {@code MigracionCifradoLogin} los convierte
 *   y para valores cifrados que todavía reciba el endpoint Layaway.</li>
 *   <li>HMAC-SHA256 con llave propia para buscar por usuario sin descifrar la tabla.</li>
 *   <li>BCrypt (con sal aleatoria por contraseña) para hashing irreversible de contraseñas. Los hashes SHA-256
 *   heredados se siguen verificando y se migran a BCrypt en el siguiente inicio de sesión.</li>
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
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String PREFIJO_V2 = "v2:";
    private static final int BYTES_LLAVE_V2 = 32;
    private static final int BYTES_IV_GCM = 12;
    private static final int BITS_TAG_GCM = 128;
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String PREFIJO_BCRYPT = "$2";
    /** BCrypt solo considera los primeros 72 bytes; contraseñas más largas se rechazan para evitar truncamientos. */
    private static final int MAX_BYTES_PASSWORD = 72;
    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final SecretKeySpec secretKeySpec;
    private final SecretKeySpec llaveCifrado;
    private final SecretKeySpec llaveBusqueda;
    private final SecureRandom random = new SecureRandom();

    /**
     * @param secretKey  llave heredada: solo se usa para leer datos en AES/ECB.
     * @param llaveV2Base64 llave maestra de 32 bytes en Base64; de ella se derivan una llave para cifrar y otra para buscar.
     */
    public CryptoService(@Value("${seguridad.crypto.secret-key}") String secretKey,
                         @Value("${seguridad.crypto.llave-v2}") String llaveV2Base64) {
        byte[] llaveMaestra = decodificarLlaveV2(llaveV2Base64);
        this.llaveCifrado = new SecretKeySpec(hmac(llaveMaestra, "proyecto/cifrado/v2"), "AES");
        this.llaveBusqueda = new SecretKeySpec(hmac(llaveMaestra, "proyecto/busqueda/v2"), HMAC_SHA256);
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

    private static byte[] decodificarLlaveV2(String llaveBase64) {
        byte[] llave;
        try {
            llave = Base64.getDecoder().decode(llaveBase64 == null ? "" : llaveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("seguridad.crypto.llave-v2 debe estar en Base64", e);
        }
        if (llave.length != BYTES_LLAVE_V2) {
            throw new IllegalStateException("seguridad.crypto.llave-v2 debe ser una llave de 32 bytes en Base64");
        }
        return llave;
    }

    private static byte[] hmac(byte[] llave, String texto) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(llave, HMAC_SHA256));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo inicializar " + HMAC_SHA256, e);
        }
    }

    /**
     * Valor determinista para buscar por usuario sin descifrar la tabla.
     *
     * @param texto usuario en claro.
     * @return HMAC-SHA256 en Base64 URL (43 caracteres), o {@code null} si el texto es {@code null}.
     */
    public String hashBusqueda(String texto) {
        if (texto == null) {
            return null;
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(llaveBusqueda.getEncoded(), texto));
    }

    /**
     * Indica si un valor guardado sigue en el formato heredado y debe volver a cifrarse.
     */
    public boolean requiereMigracion(String cifrado) {
        return cifrado != null && !cifrado.isEmpty() && !cifrado.startsWith(PREFIJO_V2);
    }

    /**
     * Cifra un texto en claro con AES-256-GCM y un IV aleatorio.
     *
     * @param plainText texto a cifrar.
     * @return cadena cifrada en formato Base64.
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        try {
            byte[] iv = new byte[BYTES_IV_GCM];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, llaveCifrado, new GCMParameterSpec(BITS_TAG_GCM, iv));
            byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] ivMasCifrado = ByteBuffer.allocate(iv.length + encryptedBytes.length).put(iv).put(encryptedBytes).array();
            return PREFIJO_V2 + Base64.getEncoder().encodeToString(ivMasCifrado);
        } catch (Exception e) {
            log.error("Error al cifrar información sensible: {}", e.getMessage());
            throw new RuntimeException("Error en proceso de cifrado de datos", e);
        }
    }

    /**
     * Descifra un texto cifrado con {@link #encrypt(String)} o, si no trae prefijo, en el formato heredado.
     *
     * @param cipherText cadena cifrada.
     * @return texto descifrado en claro.
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return cipherText;
        }
        try {
            return cipherText.startsWith(PREFIJO_V2) ? descifrarV2(cipherText) : descifrarHeredado(cipherText);
        } catch (Exception e) {
            log.error("Error al descifrar información: {}", e.getMessage());
            throw new RuntimeException("Error en proceso de descifrado de datos", e);
        }
    }

    private String descifrarV2(String cipherText) throws GeneralSecurityException {
        byte[] ivMasCifrado = Base64.getDecoder().decode(cipherText.substring(PREFIJO_V2.length()));
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, llaveCifrado, new GCMParameterSpec(BITS_TAG_GCM, ivMasCifrado, 0, BYTES_IV_GCM));
        byte[] claro = cipher.doFinal(ivMasCifrado, BYTES_IV_GCM, ivMasCifrado.length - BYTES_IV_GCM);
        return new String(claro, StandardCharsets.UTF_8);
    }

    /** Lectura del formato anterior; se elimina cuando no queden datos ni clientes que lo usen. */
    private String descifrarHeredado(String cipherText) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        return new String(cipher.doFinal(Base64.getDecoder().decode(cipherText)), StandardCharsets.UTF_8);
    }

    /**
     * Genera un hash BCrypt (con sal aleatoria) para almacenamiento de contraseñas.
     *
     * @param rawPassword contraseña en texto plano.
     * @return hash BCrypt.
     * @throws ValidacionNegocioException si la contraseña excede 72 bytes.
     */
    public String hashPassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            throw new ValidacionNegocioException("La contraseña no debe exceder " + MAX_BYTES_PASSWORD + " caracteres");
        }
        return BCRYPT.encode(rawPassword);
    }

    /**
     * Compara una contraseña en texto plano contra su hash almacenado (BCrypt o SHA-256 heredado).
     *
     * @param rawPassword contraseña enviada por el usuario.
     * @param hashedPassword hash almacenado en la base de datos.
     * @return {@code true} si coinciden, {@code false} en caso contrario.
     */
    public boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            return false;
        }
        if (hashedPassword.startsWith(PREFIJO_BCRYPT)) {
            return BCRYPT.matches(rawPassword, hashedPassword);
        }
        String generatedHash = legacySha256(rawPassword);
        return MessageDigest.isEqual(generatedHash.getBytes(StandardCharsets.UTF_8),
                hashedPassword.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Indica si el hash almacenado usa el formato anterior (SHA-256 sin sal) y debe regenerarse con BCrypt.
     */
    public boolean requiereMigracionHash(String hashedPassword) {
        return hashedPassword != null && !hashedPassword.startsWith(PREFIJO_BCRYPT);
    }

    private String legacySha256(String rawPassword) {
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
