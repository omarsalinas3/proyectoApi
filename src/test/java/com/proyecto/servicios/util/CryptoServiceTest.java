package com.proyecto.servicios.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - CryptoService")
class CryptoServiceTest {

    private CryptoService cryptoService;

    @BeforeEach
    void setUp() {
        cryptoService = new CryptoService("ClaveSecretaDePruebaParaTests2026AES");
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
}
