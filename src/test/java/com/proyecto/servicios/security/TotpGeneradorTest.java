package com.proyecto.servicios.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - TotpGenerador (RFC 6238)")
class TotpGeneradorTest {

    /** Secreto ASCII "12345678901234567890" del RFC 6238 codificado en Base32. */
    private static final String SECRETO_RFC = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    private final TotpGenerador totp = new TotpGenerador();

    @Test
    @DisplayName("Coincide con los vectores de prueba SHA1 del RFC 6238 (6 dígitos)")
    void vectoresRfc6238() {
        assertEquals("287082", totp.codigoEn(SECRETO_RFC, Instant.ofEpochSecond(59)));
        assertEquals("081804", totp.codigoEn(SECRETO_RFC, Instant.ofEpochSecond(1111111109)));
        assertEquals("050471", totp.codigoEn(SECRETO_RFC, Instant.ofEpochSecond(1111111111)));
        assertEquals("005924", totp.codigoEn(SECRETO_RFC, Instant.ofEpochSecond(1234567890)));
        assertEquals("279037", totp.codigoEn(SECRETO_RFC, Instant.ofEpochSecond(2000000000)));
    }

    @Test
    @DisplayName("Acepta el código vigente y los de ±30 s; rechaza códigos más viejos")
    void toleranciaDeReloj() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000);
        String vigente = totp.codigoEn(SECRETO_RFC, ahora);
        String anterior = totp.codigoEn(SECRETO_RFC, ahora.minusSeconds(30));
        String viejo = totp.codigoEn(SECRETO_RFC, ahora.minusSeconds(90));

        assertTrue(totp.verificar(SECRETO_RFC, vigente, null, ahora).isPresent());
        assertTrue(totp.verificar(SECRETO_RFC, anterior, null, ahora).isPresent());
        assertTrue(totp.verificar(SECRETO_RFC, viejo, null, ahora).isEmpty());
    }

    @Test
    @DisplayName("Un código ya usado no se puede reutilizar (anti-replay)")
    void antiReplay() {
        Instant ahora = Instant.ofEpochSecond(1_700_000_000);
        String codigo = totp.codigoEn(SECRETO_RFC, ahora);

        OptionalLong paso = totp.verificar(SECRETO_RFC, codigo, null, ahora);
        assertTrue(paso.isPresent());
        assertTrue(totp.verificar(SECRETO_RFC, codigo, paso.getAsLong(), ahora).isEmpty());
    }

    @Test
    @DisplayName("Rechaza códigos con formato inválido")
    void formatoInvalido() {
        Instant ahora = Instant.now();
        assertTrue(totp.verificar(SECRETO_RFC, "12345", null, ahora).isEmpty());
        assertTrue(totp.verificar(SECRETO_RFC, "abcdef", null, ahora).isEmpty());
        assertTrue(totp.verificar(SECRETO_RFC, null, null, ahora).isEmpty());
    }

    @Test
    @DisplayName("Genera secretos Base32 de 160 bits y una URI otpauth válida")
    void secretoYUri() {
        String secreto = totp.generarSecreto();
        assertTrue(secreto.matches("[A-Z2-7]{32}"), secreto);
        assertNotEquals(secreto, totp.generarSecreto());

        String uri = totp.uriProvisionamiento("ProyectoApi", "ana@example.com", secreto);
        assertTrue(uri.startsWith("otpauth://totp/ProyectoApi:ana%40example.com?secret=" + secreto), uri);
        assertTrue(uri.contains("&issuer=ProyectoApi&algorithm=SHA1&digits=6&period=30"), uri);
    }
}
