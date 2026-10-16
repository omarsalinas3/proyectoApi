package com.proyecto.servicios.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que autentica cada petición a partir del encabezado {@code Authorization: Bearer <token>}.
 *
 * <p>Para tokens de clientes además verifica en base de datos que la sesión siga activa, que no haya
 * expirado por inactividad y que el cliente no haya sido dado de baja, de modo que un logout o una baja
 * lógica invalidan el token aunque su firma siga vigente.</p>
 *
 * <p>No se registra como bean para que Spring Boot no lo agregue también a la cadena de filtros del servlet;
 * se instancia en {@link com.proyecto.servicios.config.SecurityConfig}.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Atributo de la petición donde se deja el motivo del rechazo del token para la respuesta 401. */
    public static final String ATRIBUTO_ERROR_TOKEN = "seguridad.jwt.error";

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final AuthService authService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            autenticar(header.substring(PREFIJO_BEARER.length()).trim(), request);
        }
        chain.doFilter(request, response);
    }

    private void autenticar(String token, HttpServletRequest request) {
        try {
            UsuarioAutenticado usuario = jwtService.validarToken(token);
            if (!usuario.esEjecutivo()) {
                authService.validarSesionToken(usuario.clienteId());
            }
            var authentication = new UsernamePasswordAuthenticationToken(
                    usuario, null, List.of(new SimpleGrantedAuthority(usuario.rol().authority())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (TokenExpiredException e) {
            rechazar(request, "El token de acceso ha expirado. Inicie sesión nuevamente en POST /api/v1/auth/login");
        } catch (JWTVerificationException e) {
            log.warn("Token JWT rechazado en {} {}: {}", request.getMethod(), request.getRequestURI(), e.getMessage());
            rechazar(request, "El token de acceso es inválido o fue alterado");
        } catch (SesionExpiradaException | CredencialesInvalidasException e) {
            rechazar(request, e.getMessage());
        }
    }

    private void rechazar(HttpServletRequest request, String motivo) {
        SecurityContextHolder.clearContext();
        request.setAttribute(ATRIBUTO_ERROR_TOKEN, motivo);
    }
}
