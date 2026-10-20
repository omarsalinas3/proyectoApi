package com.proyecto.servicios.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.common.RespuestaApi;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respuestas JSON estandarizadas ({@link RespuestaApi}) para los rechazos de Spring Security:
 * 401 cuando falta o es inválido el token, 403 cuando el rol no permite el recurso y 400 cuando el firewall
 * rechaza una URL maliciosa o mal formada (ej. {@code //}, {@code ..}, caracteres codificados peligrosos).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        Object motivo = request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_ERROR_TOKEN);
        String detalle = motivo != null
                ? motivo.toString()
                : "Debe enviar un token de acceso en el encabezado 'Authorization: Bearer <token>'. "
                + "Obténgalo en POST /api/v1/auth/login";
        escribir(response, HttpStatus.UNAUTHORIZED, CodigoRespuestaEnum.ERROR_NO_AUTENTICADO, detalle);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
            throws IOException {
        log.warn("Acceso denegado por rol a {} {}", request.getMethod(), request.getRequestURI());
        escribir(response, HttpStatus.FORBIDDEN, CodigoRespuestaEnum.ERROR_ACCESO_DENEGADO,
                "Su rol no tiene permisos para ejecutar esta operación; está reservada para ejecutivos");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, RequestRejectedException ex)
            throws IOException {
        log.warn("URL rechazada por el firewall: {}", ex.getMessage());
        escribir(response, HttpStatus.BAD_REQUEST, CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO,
                "La URL contiene segmentos vacíos o caracteres no permitidos");
    }

    private void escribir(HttpServletResponse response, HttpStatus status, CodigoRespuestaEnum codigo, String detalle)
            throws IOException {
        RespuestaApi<Void> error = RespuestaApi.error(codigo, detalle);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
