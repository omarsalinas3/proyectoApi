package com.proyecto.servicios.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.proyecto.servicios.enums.RolEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Emisión y validación de tokens JWT (HMAC-SHA256) para las sesiones de la API.
 *
 * <p>La clave se toma de {@code seguridad.jwt.secret} (variable {@code JWT_SECRET}). Si no está definida o
 * tiene menos de 32 bytes se genera una clave aleatoria en memoria: la API sigue siendo segura, pero los
 * tokens emitidos dejan de ser válidos cuando la aplicación se reinicia.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
public class JwtService {

    private static final String ISSUER = "proyecto-servicios";
    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_CLIENTE_ID = "cid";
    private static final int LONGITUD_MINIMA_CLAVE = 32;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final long expiracionSegundos;

    public JwtService(@Value("${seguridad.jwt.secret:}") String secret,
                      @Value("${seguridad.jwt.expiracion-minutos:30}") long expiracionMinutos) {
        if (expiracionMinutos <= 0) {
            throw new IllegalStateException("seguridad.jwt.expiracion-minutos debe ser mayor a 0");
        }
        byte[] clave;
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < LONGITUD_MINIMA_CLAVE) {
            log.warn("JWT_SECRET no está configurado o tiene menos de {} bytes: se usará una clave aleatoria "
                    + "y los tokens dejarán de ser válidos al reiniciar la aplicación", LONGITUD_MINIMA_CLAVE);
            clave = new byte[64];
            new SecureRandom().nextBytes(clave);
        } else {
            clave = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.algorithm = Algorithm.HMAC256(clave);
        this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
        this.expiracionSegundos = expiracionMinutos * 60;
    }

    /**
     * Genera un token firmado para el usuario indicado.
     */
    public String generarToken(UsuarioAutenticado usuario) {
        Instant ahora = Instant.now();
        var builder = JWT.create()
                .withIssuer(ISSUER)
                .withJWTId(UUID.randomUUID().toString())
                .withSubject(usuario.username())
                .withClaim(CLAIM_ROL, usuario.rol().name())
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plusSeconds(expiracionSegundos));
        if (usuario.clienteId() != null) {
            builder.withClaim(CLAIM_CLIENTE_ID, usuario.clienteId());
        }
        return builder.sign(algorithm);
    }

    /**
     * Valida firma, emisor y expiración del token y devuelve la identidad que contiene.
     *
     * @throws JWTVerificationException si el token es inválido, fue alterado o expiró.
     */
    public UsuarioAutenticado validarToken(String token) {
        DecodedJWT jwt = verifier.verify(token);
        RolEnum rol;
        try {
            rol = RolEnum.valueOf(jwt.getClaim(CLAIM_ROL).asString());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new JWTDecodeException("El token no contiene un rol válido");
        }
        Long clienteId = jwt.getClaim(CLAIM_CLIENTE_ID).asLong();
        if (rol == RolEnum.CLIENTE && clienteId == null) {
            throw new JWTDecodeException("El token de cliente no contiene el identificador del cliente");
        }
        return new UsuarioAutenticado(clienteId, jwt.getSubject(), rol);
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }
}
