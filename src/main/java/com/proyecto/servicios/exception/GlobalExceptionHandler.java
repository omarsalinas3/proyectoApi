package com.proyecto.servicios.exception;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import feign.FeignException;
import feign.RetryableException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
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
                .map(fe -> "[" + fe.getField() + "] " + fe.getDefaultMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en los datos de entrada: {}", detalles);
        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO)
                .codigo(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO.getCodigo())
                .mensaje(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO.getDescripcion())
                .detalle(detalles)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja errores cuando el formato JSON contiene tipos incompatibles (ej. comillas en campos numéricos como bandera).
     * Retorna HTTP 404 para coincidir con la documentación y evitar respuestas indocumentadas.
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        String detalle = "El cuerpo de la petición contiene un formato JSON inválido o tipos de datos incompatibles";
        Throwable rootCause = ex.getRootCause() != null ? ex.getRootCause() : ex.getCause();

        if (rootCause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException mie) {
            String fieldName = mie.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.joining("."));

            if (fieldName != null && !fieldName.isBlank()) {
                detalle = String.format("El campo '%s' debe ser un valor numérico directo (sin comillas). No se permiten cadenas de texto para valores numéricos o banderas", fieldName);
            } else {
                detalle = "Tipo de dato no coincidente en la petición JSON. Verifique que los valores numéricos no lleven comillas";
            }
        } else if (rootCause instanceof com.fasterxml.jackson.core.JsonProcessingException jpe && jpe.getLocation() != null) {
            // No se devuelve el mensaje interno del parser; solo la posición del error para que el consumidor lo corrija
            detalle = String.format("El cuerpo de la petición no es un JSON válido (línea %d, columna %d). Revise comillas, comas y llaves",
                    jpe.getLocation().getLineNr(), jpe.getLocation().getColumnNr());
        }

        log.warn("Error de deserialización JSON (HTTP 404): {}", detalle);
        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.ERROR_CLIENTE_NO_ENCONTRADO)
                .codigo(HttpStatus.NOT_FOUND.value())
                .mensaje("Bandera o parámetro no encontrado / formato no válido")
                .detalle(detalle)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
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
     * Maneja parámetros de ruta o de consulta que no cumplen sus restricciones (ej. {@code id <= 0},
     * CURP con formato inválido). Se valida antes de ejecutar cualquier lógica de negocio.
     *
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        String detalles = ex.getAllValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> "[" + r.getMethodParameter().getParameterName() + "] " + e.getDefaultMessage()))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Parámetros de ruta/consulta inválidos: {}", detalles);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, detalles, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja violaciones de restricciones lanzadas por validación de métodos ({@code @Validated}).
     *
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Set<ConstraintViolation<?>> violaciones = ex.getConstraintViolations();
        String detalles = violaciones.stream()
                .map(v -> "[" + ultimoNodo(v.getPropertyPath()) + "] " + v.getMessage())
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        log.warn("Restricciones de validación incumplidas: {}", detalles);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, detalles, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja parámetros con tipo incorrecto (ej. {@code /clientes/abc} cuando se espera un ID numérico).
     *
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> tipo = ex.getRequiredType();
        String esperado;
        if (tipo != null && LocalDate.class.isAssignableFrom(tipo)) {
            esperado = "una fecha con formato YYYY-MM-DD";
        } else if (tipo != null && Number.class.isAssignableFrom(tipo)) {
            esperado = "un número entero válido";
        } else {
            esperado = "un valor de tipo " + (tipo != null ? tipo.getSimpleName() : "válido");
        }
        String valor = String.valueOf(ex.getValue());
        if (valor.length() > 50) {
            valor = valor.substring(0, 50) + "...";
        }
        String detalle = String.format("[%s] El parámetro '%s' debe ser %s (valor recibido: '%s')",
                ex.getName(), ex.getName(), esperado, valor);
        log.warn("Tipo de parámetro inválido: {}", detalle);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, detalle, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja la ausencia de parámetros de consulta obligatorios.
     *
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        String detalle = String.format("[%s] El parámetro '%s' es obligatorio", ex.getParameterName(), ex.getParameterName());
        log.warn("Parámetro obligatorio ausente: {}", ex.getParameterName());
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, detalle, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja parámetros semánticamente inválidos detectados en el controlador (ej. rango de fechas invertido).
     *
     * @return respuesta con código HTTP 400 Bad Request.
     */
    @ExceptionHandler(ParametroInvalidoException.class)
    public ResponseEntity<ErrorResponse> handleParametroInvalido(ParametroInvalidoException ex) {
        log.warn("Parámetro inválido: {}", ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja peticiones protegidas sin un token válido.
     *
     * @return respuesta con código HTTP 401 Unauthorized.
     */
    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<ErrorResponse> handleNoAutenticado(NoAutenticadoException ex) {
        return construir(CodigoRespuestaEnum.ERROR_NO_AUTENTICADO, ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja intentos de acceder a registros de otro cliente o a operaciones reservadas para ejecutivos.
     *
     * @return respuesta con código HTTP 403 Forbidden.
     */
    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAccesoDenegado(RuntimeException ex) {
        String detalle = ex instanceof AccesoDenegadoException
                ? ex.getMessage()
                : "No tiene permisos para ejecutar esta operación";
        return construir(CodigoRespuestaEnum.ERROR_ACCESO_DENEGADO, detalle, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja rutas inexistentes (ej. {@code GET /}) para responder 404 en lugar de error interno.
     *
     * @return respuesta con código HTTP 404 Not Found.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        HttpMethod metodo = ex.getHttpMethod();
        String detalle = String.format("No existe el recurso %s /%s. Consulte la documentación en /swagger-ui.html",
                metodo != null ? metodo.name() : "", ex.getResourcePath());
        return construir(CodigoRespuestaEnum.ERROR_RECURSO_NO_ENCONTRADO, detalle, HttpStatus.NOT_FOUND);
    }

    /**
     * Maneja métodos HTTP no soportados por el recurso.
     *
     * @return respuesta con código HTTP 405 Method Not Allowed.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String detalle = String.format("El método %s no está permitido para este recurso. Métodos permitidos: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return construir(CodigoRespuestaEnum.ERROR_METODO_NO_PERMITIDO, detalle, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * Maneja peticiones con un {@code Content-Type} distinto de JSON.
     *
     * @return respuesta con código HTTP 415 Unsupported Media Type.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String detalle = String.format("El tipo de contenido '%s' no es soportado. Use 'Content-Type: %s'",
                ex.getContentType(), MediaType.APPLICATION_JSON_VALUE);
        return construir(CodigoRespuestaEnum.ERROR_TIPO_CONTENIDO, detalle, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    /**
     * Maneja violaciones de restricciones de base de datos (ej. dos altas simultáneas con la misma CURP)
     * sin exponer el detalle SQL al consumidor.
     *
     * @return respuesta con código HTTP 409 Conflict.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return construir(CodigoRespuestaEnum.ERROR_CONFLICTO_DATOS,
                "El registro entra en conflicto con información existente (dato único duplicado o referencia inválida)",
                HttpStatus.CONFLICT);
    }

    private ResponseEntity<ErrorResponse> construir(CodigoRespuestaEnum codigo, String detalle, HttpStatusCode status) {
        ErrorResponse error = ErrorResponse.builder()
                .codigoRespuesta(codigo)
                .codigo(codigo.getCodigo())
                .mensaje(codigo.getDescripcion())
                .detalle(detalle)
                .timestamp(LocalDateTime.now())
                .build();
        return new ResponseEntity<>(error, status);
    }

    private static String ultimoNodo(Path path) {
        String nombre = null;
        for (Path.Node nodo : path) {
            nombre = nodo.getName();
        }
        return nombre != null ? nombre : path.toString();
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
