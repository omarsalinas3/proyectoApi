package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - CryptoService")
class CryptoServiceTest {

    private static final String LLAVE_HEREDADA = "ClaveSecretaDePruebaParaTests2026AES";
    private static final String LLAVE_V2 = Base64.getEncoder().encodeToString(new byte[32]);

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService(LLAVE_HEREDADA, LLAVE_V2);
    }

    @Test
    @DisplayName("Debe cifrar y descifrar un texto en claro correctamente con AES")
    void testEncryptDecryptText() {
        String original = "juan.perez@banco.com";
        String encrypted = cryptoService.encrypt(original);

        assertNotNull(encrypted);
        assertNotEquals(original, encrypted);

        String decrypted = cryptoService.decrypt(encrypted);
        assertEquals(original, decrypted);
    }

    @Test
    @DisplayName("Debe generar un hash seguro de contraseña y validarlo correctamente")
    void testPasswordHashingAndVerification() {
        String rawPassword = "PasswordSeguro2026!";
        String hash = cryptoService.hashPassword(rawPassword);

        assertNotNull(hash);
        assertNotEquals(rawPassword, hash);
        assertTrue(cryptoService.verifyPassword(rawPassword, hash));
        assertFalse(cryptoService.verifyPassword("PasswordIncorrecto", hash));
    }

    @Test
    @DisplayName("Debe cifrar y descifrar datos biométricos preservando la precisión exacta de BigDecimal")
    void testEncryptDecryptBiometricsPrecision() {
        BigDecimal biometriaOriginal = new BigDecimal("0.987654321012345678901234567890");
        String encrypted = cryptoService.encryptBiometrics(biometriaOriginal);

        assertNotNull(encrypted);
        BigDecimal biometriaDecrypted = cryptoService.decryptBiometrics(encrypted);

        assertNotNull(biometriaDecrypted);
        assertEquals(biometriaOriginal, biometriaDecrypted);
    }

    @Test
    @DisplayName("Debe manejar valores nulos en cifrado y descifrado de forma segura")
    void testNullHandling() {
        assertNull(cryptoService.encrypt(null));
        assertNull(cryptoService.decrypt(null));
        assertNull(cryptoService.hashPassword(null));
        assertFalse(cryptoService.verifyPassword(null, "hash"));
        assertNull(cryptoService.encryptBiometrics(null));
        assertNull(cryptoService.decryptBiometrics(null));
    }

    @Test
    @DisplayName("Las contraseñas se guardan con BCrypt (sal aleatoria) y los hashes SHA-256 heredados se siguen validando")
    void testBcryptYMigracionLegacy() {
        String hash1 = cryptoService.hashPassword("PasswordSeguro2026!");
        String hash2 = cryptoService.hashPassword("PasswordSeguro2026!");
        assertTrue(hash1.startsWith("$2"));
        assertNotEquals(hash1, hash2);
        assertFalse(cryptoService.requiereMigracionHash(hash1));

        // SHA-256 hexadecimal de "abc" (formato anterior)
        String legacy = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
        assertTrue(cryptoService.verifyPassword("abc", legacy));
        assertFalse(cryptoService.verifyPassword("abd", legacy));
        assertTrue(cryptoService.requiereMigracionHash(legacy));
    }

    @Test
    @DisplayName("Debe rechazar contraseñas de más de 72 bytes (límite de BCrypt)")
    void testPasswordDemasiadoLarga() {
        String larga = "A1!" + "x".repeat(80);
        assertThrows(com.proyecto.servicios.exception.ValidacionNegocioException.class, () -> cryptoService.hashPassword(larga));
        assertFalse(cryptoService.verifyPassword(larga, cryptoService.hashPassword("Corta123!")));
    }

    /** Reproduce el cifrado anterior (AES/ECB con llave derivada de SHA-256) para probar la lectura de datos viejos. */
    private static String cifrarComoAntes(String texto) throws Exception {
        byte[] llave = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(LLAVE_HEREDADA.getBytes(StandardCharsets.UTF_8)), 16);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(llave, "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Cifrar dos veces el mismo texto da resultados distintos y ambos se pueden leer")
    void cifradoNoDeterminista() {
        String a = cryptoService.encrypt("juan.perez");
        String b = cryptoService.encrypt("juan.perez");

        assertNotEquals(a, b);
        assertTrue(a.startsWith("v2:"));
        assertEquals("juan.perez", cryptoService.decrypt(a));
        assertEquals("juan.perez", cryptoService.decrypt(b));
    }

    @Test
    @DisplayName("Un texto cifrado alterado se rechaza en lugar de devolver datos corruptos")
    void textoAlteradoSeRechaza() {
        String cifrado = cryptoService.encrypt("juan.perez");
        byte[] bytes = Base64.getDecoder().decode(cifrado.substring(3));
        bytes[bytes.length - 1] ^= 1;
        String alterado = "v2:" + Base64.getEncoder().encodeToString(bytes);

        assertThrows(RuntimeException.class, () -> cryptoService.decrypt(alterado));
    }

    @Test
    @DisplayName("Sigue leyendo los datos guardados con el cifrado anterior")
    void leeDatosHeredados() throws Exception {
        String heredado = cifrarComoAntes("usuario.viejo");

        assertTrue(cryptoService.requiereMigracion(heredado));
        assertEquals("usuario.viejo", cryptoService.decrypt(heredado));
        assertFalse(cryptoService.requiereMigracion(cryptoService.encrypt("x")));
        assertFalse(cryptoService.requiereMigracion(null));
    }

    @Test
    @DisplayName("El hash de búsqueda es estable, distingue usuarios y depende de la llave")
    void hashDeBusqueda() {
        String hash = cryptoService.hashBusqueda("juan.perez");

        assertEquals(hash, cryptoService.hashBusqueda("juan.perez"));
        assertNotEquals(hash, cryptoService.hashBusqueda("juan.perez2"));
        assertTrue(hash.length() <= 64);
        assertNull(cryptoService.hashBusqueda(null));

        byte[] otra = new byte[32];
        otra[0] = 1;
        CryptoService otroAmbiente = new CryptoService(LLAVE_HEREDADA, Base64.getEncoder().encodeToString(otra));
        assertNotEquals(hash, otroAmbiente.hashBusqueda("juan.perez"));
    }

    @Test
    @DisplayName("No arranca con una llave nueva ausente, inválida o de tamaño incorrecto")
    void llaveV2Invalida() {
        String llaveCorta = Base64.getEncoder().encodeToString(new byte[16]);

        assertThrows(IllegalStateException.class, () -> new CryptoService(LLAVE_HEREDADA, ""));
        assertThrows(IllegalStateException.class, () -> new CryptoService(LLAVE_HEREDADA, "no-es-base64!"));
        assertThrows(IllegalStateException.class, () -> new CryptoService(LLAVE_HEREDADA, llaveCorta));
    }
}
