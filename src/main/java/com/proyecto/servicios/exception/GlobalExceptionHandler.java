package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
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
 *   <li>Transformar los errores en respuestas estandarizadas {@link ErrorResponse} con {@link CodigoRespuestaEnum}.</li>
 *   <li>Registrar eventos de error en los logs con SLF4J protegiendo credenciales y datos sensibles.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_AUTENTICACION)
                .codigo(CodigoRespuestaEnum.ERROR_AUTENTICACION.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_AUTENTICACION.getDescripcion())
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_TIMEOUT)
                .codigo(CodigoRespuestaEnum.ERROR_TIMEOUT.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_TIMEOUT.getDescripcion())
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO)
                .codigo(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getDescripcion())
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO)
                .codigo(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO.getDescripcion())
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_INTERNO)
                .codigo(HttpStatus.BAD_REQUEST.value())
                .mensaje("Parámetros de entrada inválidos")
                .detalle(detalles)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores cuando no se encuentra un cliente o cuenta bancaria.
     *
     * @param ex excepción capturada de tipo {@link ClienteNoEncontradoException} o {@link CuentaNoEncontradaException}.
     * @return respuesta con código HTTP 404 Not Found.
     */
    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<ErrorResponse> handleNotFoundException(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        CodigoRespuestaEnum codigoEnum = ex instanceof ClienteNoEncontradoException 
                ? CodigoRespuestaEnum.ERROR_CLIENTE_NO_ENCONTRADO 
                : CodigoRespuestaEnum.ERROR_CUENTA_NO_ENCONTRADA;

        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(codigoEnum)
                .codigo(codigoEnum.getCodigo())
                .mensaje(codigoEnum.getDescripcion())
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja errores por duplicidad de registros únicos (CURP, RFC, Correo, Usuario).
     *
     * @param ex excepción capturada de duplicidad.
     * @return respuesta con código HTTP 409 Conflict.
     */
    @ExceptionHandler({
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            CorreoDuplicadoException.class,
            UsuarioDuplicadoException.class
    })
    public ResponseEntity<ErrorResponse> handleDuplicityException(RuntimeException ex) {
        log.warn("Conflicto de unicidad detectado: {}", ex.getMessage());
        CodigoRespuestaEnum codigoEnum;
        if (ex instanceof CurpDuplicadaException) {
            codigoEnum = CodigoRespuestaEnum.ERROR_CURP_DUPLICADA;
        } else if (ex instanceof RfcDuplicadoException) {
            codigoEnum = CodigoRespuestaEnum.ERROR_RFC_DUPLICADO;
        } else if (ex instanceof CorreoDuplicadoException) {
            codigoEnum = CodigoRespuestaEnum.ERROR_CORREO_DUPLICADO;
        } else {
            codigoEnum = CodigoRespuestaEnum.ERROR_USUARIO_DUPLICADO;
        }

        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(codigoEnum)
                .codigo(codigoEnum.getCodigo())
                .mensaje(codigoEnum.getDescripcion())
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Maneja errores de credenciales inválidas y expiración de sesiones.
     *
     * @param ex excepción capturada de autenticación o sesión.
     * @return respuesta con código HTTP 401 Unauthorized.
     */
    @ExceptionHandler({CredencialesInvalidasException.class, SesionExpiradaException.class})
    public ResponseEntity<ErrorResponse> handleAuthSessionException(RuntimeException ex) {
        log.warn("Fallo de acceso o sesión: {}", ex.getMessage());
        CodigoRespuestaEnum codigoEnum = ex instanceof SesionExpiradaException 
                ? CodigoRespuestaEnum.ERROR_SESION_EXPIRADA 
                : CodigoRespuestaEnum.ERROR_CREDENCIALES_INVALIDAS;

        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(codigoEnum)
                .codigo(codigoEnum.getCodigo())
                .mensaje(codigoEnum.getDescripcion())
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja violaciones a reglas de negocio (ej. modificación de campos inmutables).
     *
     * @param ex excepción capturada de tipo {@link ValidacionNegocioException}.
     * @return respuesta con código HTTP 422 Unprocessable Entity.
     */
    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<ErrorResponse> handleBusinessValidationException(ValidacionNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_VALIDACION_NEGOCIO)
                .codigo(CodigoRespuestaEnum.ERROR_VALIDACION_NEGOCIO.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_VALIDACION_NEGOCIO.getDescripcion())
                .detalle(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNPROCESSABLE_ENTITY);
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
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_INTERNO)
                .codigo(CodigoRespuestaEnum.ERROR_INTERNO.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_INTERNO.getDescripcion())
                .detalle("Por favor intente más tarde o consulte con soporte técnico")
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
