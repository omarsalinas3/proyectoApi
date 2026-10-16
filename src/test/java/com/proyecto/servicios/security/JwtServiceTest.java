package com.proyecto.servicios.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.proyecto.servicios.enums.RolEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - JwtService")
class JwtServiceTest {

    private static final String SECRETO = "clave-de-prueba-con-al-menos-32-bytes-1234";

    private final JwtService jwtService = new JwtService(SECRETO, 30);

    @Test
    @DisplayName("Debe generar y validar un token conservando cliente, usuario y rol")
    void generarYValidar() {
        String token = jwtService.generarToken(new UsuarioAutenticado(7L, "ana.morales@example.com", RolEnum.CLIENTE));

        UsuarioAutenticado usuario = jwtService.validarToken(token);

        assertEquals(7L, usuario.clienteId());
        assertEquals("ana.morales@example.com", usuario.username());
        assertEquals(RolEnum.CLIENTE, usuario.rol());
        assertEquals(1800, jwtService.getExpiracionSegundos());
    }

    @Test
    @DisplayName("Debe rechazar un token alterado")
    void rechazaTokenAlterado() {
        String token = jwtService.generarToken(new UsuarioAutenticado(7L, "ana", RolEnum.CLIENTE));
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        assertThrows(JWTVerificationException.class, () -> jwtService.validarToken(alterado));
    }

    @Test
    @DisplayName("Debe rechazar un token firmado con otra clave")
    void rechazaTokenDeOtraClave() {
        JwtService otro = new JwtService("otra-clave-distinta-de-al-menos-32-bytes-xyz", 30);
        String token = otro.generarToken(new UsuarioAutenticado(null, "ejecutivo", RolEnum.EJECUTIVO));

        assertThrows(JWTVerificationException.class, () -> jwtService.validarToken(token));
    }

    @Test
    @DisplayName("Sin JWT_SECRET usa una clave aleatoria y sigue funcionando")
    void claveAleatoriaSinSecreto() {
        JwtService sinSecreto = new JwtService("", 30);
        String token = sinSecreto.generarToken(new UsuarioAutenticado(1L, "ana", RolEnum.CLIENTE));

        assertEquals(1L, sinSecreto.validarToken(token).clienteId());
        assertThrows(JWTVerificationException.class, () -> jwtService.validarToken(token));
    }

    @Test
    @DisplayName("No debe aceptar una vigencia menor o igual a cero")
    void vigenciaInvalida() {
        assertThrows(IllegalStateException.class, () -> new JwtService(SECRETO, 0));
    }

    @Test
    @DisplayName("Debe distinguir un token expirado")
    void tokenExpirado() {
        String expirado = com.auth0.jwt.JWT.create()
                .withIssuer("proyecto-servicios")
                .withSubject("ana")
                .withClaim("rol", "CLIENTE")
                .withClaim("cid", 1L)
                .withExpiresAt(java.time.Instant.now().minusSeconds(60))
                .sign(com.auth0.jwt.algorithms.Algorithm.HMAC256(SECRETO));

        assertThrows(TokenExpiredException.class, () -> jwtService.validarToken(expirado));
    }
}
