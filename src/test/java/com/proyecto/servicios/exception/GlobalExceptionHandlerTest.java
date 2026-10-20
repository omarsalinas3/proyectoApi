package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.common.RespuestaApi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el manejador global de excepciones {@link GlobalExceptionHandler}.
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@DisplayName("Pruebas Unitarias - GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Debe retornar HTTP 401 Unauthorized ante ExternalServiceAuthException")
    void handleExternalServiceAuthException() {
        // Arrange
        ExternalServiceAuthException ex = new ExternalServiceAuthException("Token inválido", 401);

        // Act
        ResponseEntity<RespuestaApi<Void>> response = exceptionHandler.handleExternalServiceAuthException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.ERROR_AUTENTICACION, response.getBody().getCodigoRespuesta());
        assertEquals(CodigoRespuestaEnum.ERROR_AUTENTICACION.getCodigo(), response.getBody().getCodigo());
        assertEquals(CodigoRespuestaEnum.ERROR_AUTENTICACION.getDescripcion(), response.getBody().getMensaje());
        assertEquals("Token inválido", response.getBody().getDetalle());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Debe retornar HTTP 504 Gateway Timeout ante ExternalServiceTimeoutException")
    void handleTimeoutException() {
        // Arrange
        ExternalServiceTimeoutException ex = new ExternalServiceTimeoutException("Timeout de lectura");

        // Act
        ResponseEntity<RespuestaApi<Void>> response = exceptionHandler.handleTimeoutException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.GATEWAY_TIMEOUT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.ERROR_TIMEOUT, response.getBody().getCodigoRespuesta());
        assertEquals(CodigoRespuestaEnum.ERROR_TIMEOUT.getCodigo(), response.getBody().getCodigo());
        assertEquals(CodigoRespuestaEnum.ERROR_TIMEOUT.getDescripcion(), response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }

    @Test
    @DisplayName("Debe retornar HTTP 502 Bad Gateway ante ExternalServiceException")
    void handleExternalServiceException() {
        // Arrange
        ExternalServiceException ex = new ExternalServiceException("Error en proveedor", 502);

        // Act
        ResponseEntity<RespuestaApi<Void>> response = exceptionHandler.handleExternalServiceException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_GATEWAY, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO, response.getBody().getCodigoRespuesta());
        assertEquals(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getCodigo(), response.getBody().getCodigo());
        assertEquals(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getDescripcion(), response.getBody().getMensaje());
        assertEquals("Error en proveedor", response.getBody().getDetalle());
    }

    @Test
    @DisplayName("Debe retornar HTTP 500 Internal Server Error ante excepciones genéricas no controladas")
    void handleGenericException() {
        // Arrange
        RuntimeException ex = new RuntimeException("Error inesperado en base de datos");

        // Act
        ResponseEntity<RespuestaApi<Void>> response = exceptionHandler.handleGenericException(ex);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(CodigoRespuestaEnum.ERROR_INTERNO, response.getBody().getCodigoRespuesta());
        assertEquals(CodigoRespuestaEnum.ERROR_INTERNO.getCodigo(), response.getBody().getCodigo());
        assertEquals(CodigoRespuestaEnum.ERROR_INTERNO.getDescripcion(), response.getBody().getMensaje());
        assertNotNull(response.getBody().getTimestamp());
    }
}
