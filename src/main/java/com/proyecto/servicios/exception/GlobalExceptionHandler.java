package com.proyecto.servicios.exception;

import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Manejador global centralizado de excepciones para la aplicación.
 *
 * <p><strong>Responsabilidades:</strong>
 * <ul>
 *   <li>Interceptar excepciones de integración, autenticación, timeouts y validaciones.</li>
 *   <li>Transformar los errores en respuestas estandarizadas {@link ErrorResponse}.</li>
 *   <li>Registrar eventos de error en los logs con SLF4J protegiendo credenciales y datos sensibles.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Maneja errores de autenticación y permisos con servicios externos (HTTP 401 / 403).
     *
     * @param ex excepción capturada de tipo {@link ExternalServiceAuthException}.
     * @return respuesta con código HTTP 401 Unauthorized.
     */
    @ExceptionHandler(ExternalServiceAuthException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceAuthException(ExternalServiceAuthException ex) {
        log.error("Fallo de autenticación con el servicio externo: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.UNAUTHORIZED.value())
                .mensaje("Fallo de autenticación en servicio externo")
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja errores por exceso de tiempo de espera (timeouts de conexión o lectura).
     *
     * @param ex excepción capturada de tipo {@link ExternalServiceTimeoutException}.
     * @return respuesta con código HTTP 504 Gateway Timeout.
     */
    @ExceptionHandler({ExternalServiceTimeoutException.class, RetryableException.class})
    public ResponseEntity<ErrorResponse> handleTimeoutException(Exception ex) {
        log.error("Timeout de comunicación con servicio externo: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.GATEWAY_TIMEOUT.value())
                .mensaje("Tiempo de espera agotado al comunicar con el servicio externo")
                .detalle("El servicio externo no respondió dentro del tiempo límite configurado")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * Maneja errores generales de integración con servicios externos.
     *
     * @param ex excepción capturada de tipo {@link ExternalServiceException}.
     * @return respuesta con código HTTP 502 Bad Gateway.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex) {
        log.error("Error devuelto por el servicio externo (status {}): {}", ex.getStatusCode(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.BAD_GATEWAY.value())
                .mensaje("Error en el servicio externo")
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Maneja errores genéricos de Feign Client no capturados por el decoder.
     *
     * @param ex excepción capturada de tipo {@link FeignException}.
     * @return respuesta con código HTTP 502 Bad Gateway.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeignException(FeignException ex) {
        log.error("Error de cliente Feign [status {}]: {}", ex.status(), ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.BAD_GATEWAY.value())
                .mensaje("Fallo de comunicación con la API externa")
                .detalle("Respuesta no satisfactoria del servicio remoto")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Maneja errores de validación de campos en las peticiones (@Valid / @NotBlank / @NotNull).
     *
     * @param ex excepción capturada de tipo {@link MethodArgumentNotValidException}.
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        log.warn("Error de validación en los datos de entrada: {}", detalles);
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Parámetros de entrada inválidos")
                .detalle(detalles)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Manejador genérico para cualquier excepción no controlada en la aplicación.
     *
     * @param ex excepción genérica no controlada.
     * @return respuesta con código HTTP 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Error interno no controlado en el servidor: ", ex);
        ErrorResponse error = ErrorResponse.builder()
                .codigo(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .mensaje("Ocurrió un error interno al procesar la solicitud")
                .detalle("Por favor intente más tarde o consulte con soporte técnico")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
