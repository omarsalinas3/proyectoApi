package com.proyecto.servicios.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.model.common.RespuestaApi;
import feign.FeignException;
import feign.RetryableException;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDate;
import java.time.temporal.Temporal;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Manejador global centralizado de excepciones.
 *
 * <p>Convierte cada tipo de error en una respuesta {@link RespuestaApi} con el código HTTP adecuado, un
 * mensaje específico y, para errores de validación, el mapa {@code errores} con el motivo de cada campo.
 * Los controladores no contienen bloques try-catch ni validaciones manuales: lanzan excepciones o delegan
 * en {@code @Valid}, y este componente las traduce. Nunca se devuelven trazas, SQL ni mensajes internos.</p>
 *
 * @author Equipo de Desarrollo
 * @version 3.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─────────────────────────────
    // 400 - DATOS DE ENTRADA
    // ─────────────────────────────

    /**
     * Errores de Bean Validation en los Request DTO anotados con {@code @Valid} (equivalente al ValidadorCampos de
     * referencia): devuelve cada campo inválido con su mensaje.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaApi<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new TreeMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.merge(fieldError.getField(), mensaje(fieldError), (a, b) -> a + "; " + b);
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            errores.merge(globalError.getObjectName(), mensaje(globalError), (a, b) -> a + "; " + b);
        }
        log.warn("Datos de entrada inválidos: {}", errores);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO,
                String.format("La petición tiene %d campo(s) con errores de validación; revise 'errores'", errores.size()),
                errores, HttpStatus.BAD_REQUEST);
    }

    /**
     * Restricciones incumplidas fuera de un Request DTO (validación de métodos).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RespuestaApi<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errores = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(v -> ultimoNodo(v.getPropertyPath()), v -> v.getMessage(),
                        (a, b) -> a + "; " + b, TreeMap::new));
        log.warn("Restricciones de validación incumplidas: {}", errores);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO,
                String.format("La petición tiene %d campo(s) con errores de validación; revise 'errores'", errores.size()),
                errores, HttpStatus.BAD_REQUEST);
    }

    /**
     * Restricciones declaradas directamente sobre parámetros de métodos de controlador.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<RespuestaApi<Void>> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        Map<String, String> errores = new TreeMap<>();
        ex.getAllValidationResults().forEach(r -> r.getResolvableErrors().forEach(e ->
                errores.merge(r.getMethodParameter().getParameterName(), Objects.toString(e.getDefaultMessage(), ""),
                        (a, b) -> a + "; " + b)));
        log.warn("Parámetros inválidos: {}", errores);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO,
                String.format("La petición tiene %d campo(s) con errores de validación; revise 'errores'", errores.size()),
                errores, HttpStatus.BAD_REQUEST);
    }

    /**
     * Cuerpo ausente, JSON mal formado o valores con un tipo de dato incompatible (ej. texto en un campo numérico,
     * fecha con formato incorrecto). Solo se informa el campo y el formato esperado, no el mensaje del parser.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaApi<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        // Se usa la excepción de Jackson (no la causa más profunda, que puede ser p. ej. DateTimeParseException)
        // porque es la que conoce el campo y el tipo esperado.
        Throwable causa = excepcionJson(ex);
        String detalle;
        Map<String, String> errores = null;

        if (causa instanceof MismatchedInputException mie && mie.getPath().isEmpty()) {
            detalle = "El cuerpo de la petición es obligatorio y debe ser un objeto JSON válido";
        } else if (causa instanceof MismatchedInputException mie) {
            String campo = rutaJson(mie);
            String motivo = String.format("Debe ser %s", descripcionTipo(mie.getTargetType()));
            errores = Map.of(campo, motivo);
            detalle = String.format("El campo '%s' tiene un tipo de dato o formato inválido", campo);
        } else if (causa instanceof JsonProcessingException jpe && jpe.getLocation() != null) {
            detalle = String.format("El cuerpo de la petición no es un JSON válido (línea %d, columna %d). "
                    + "Revise comillas, comas y llaves", jpe.getLocation().getLineNr(), jpe.getLocation().getColumnNr());
        } else {
            detalle = "El cuerpo de la petición es obligatorio y debe ser un objeto JSON válido";
        }
        log.warn("Cuerpo de petición ilegible: {}", detalle);
        return construir(CodigoRespuestaEnum.ERROR_PARAMETRO_INVALIDO, detalle, errores, HttpStatus.BAD_REQUEST);
    }

    // ─────────────────────────────
    // 401 / 403 - AUTENTICACIÓN Y AUTORIZACIÓN
    // ─────────────────────────────

    /**
     * Credenciales incorrectas, cliente dado de baja o sesión expirada.
     */
    @ExceptionHandler({CredencialesInvalidasException.class, SesionExpiradaException.class})
    public ResponseEntity<RespuestaApi<Void>> handleAuthSessionException(RuntimeException ex) {
        log.warn("Fallo de acceso o sesión: {}", ex.getMessage());
        CodigoRespuestaEnum codigo = ex instanceof SesionExpiradaException
                ? CodigoRespuestaEnum.ERROR_SESION_EXPIRADA
                : CodigoRespuestaEnum.ERROR_CREDENCIALES_INVALIDAS;
        return construir(codigo, ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Operación protegida invocada sin un token válido.
     */
    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<RespuestaApi<Void>> handleNoAutenticado(NoAutenticadoException ex) {
        return construir(CodigoRespuestaEnum.ERROR_NO_AUTENTICADO, ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Intento de operar sobre registros de otro cliente u operación no permitida para el rol.
     */
    @ExceptionHandler({AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<RespuestaApi<Void>> handleAccesoDenegado(RuntimeException ex) {
        String detalle = ex instanceof AccesoDenegadoException
                ? ex.getMessage()
                : "No tiene permisos para ejecutar esta operación";
        return construir(CodigoRespuestaEnum.ERROR_ACCESO_DENEGADO, detalle, null, HttpStatus.FORBIDDEN);
    }

    // ─────────────────────────────
    // 404 / 409 / 422 - NEGOCIO
    // ─────────────────────────────

    /**
     * Cliente o cuenta inexistente.
     */
    @ExceptionHandler({ClienteNoEncontradoException.class, CuentaNoEncontradaException.class})
    public ResponseEntity<RespuestaApi<Void>> handleNotFoundException(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        CodigoRespuestaEnum codigo = ex instanceof ClienteNoEncontradoException
                ? CodigoRespuestaEnum.ERROR_CLIENTE_NO_ENCONTRADO
                : CodigoRespuestaEnum.ERROR_CUENTA_NO_ENCONTRADA;
        return construir(codigo, ex.getMessage(), null, HttpStatus.NOT_FOUND);
    }

    /**
     * Duplicidad de CURP, RFC, correo o nombre de usuario.
     */
    @ExceptionHandler({
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            CorreoDuplicadoException.class,
            UsuarioDuplicadoException.class
    })
    public ResponseEntity<RespuestaApi<Void>> handleDuplicityException(RuntimeException ex) {
        log.warn("Conflicto de unicidad detectado: {}", ex.getMessage());
        CodigoRespuestaEnum codigo;
        if (ex instanceof CurpDuplicadaException) {
            codigo = CodigoRespuestaEnum.ERROR_CURP_DUPLICADA;
        } else if (ex instanceof RfcDuplicadoException) {
            codigo = CodigoRespuestaEnum.ERROR_RFC_DUPLICADO;
        } else if (ex instanceof CorreoDuplicadoException) {
            codigo = CodigoRespuestaEnum.ERROR_CORREO_DUPLICADO;
        } else {
            codigo = CodigoRespuestaEnum.ERROR_USUARIO_DUPLICADO;
        }
        return construir(codigo, ex.getMessage(), null, HttpStatus.CONFLICT);
    }

    /**
     * Violación de restricciones de base de datos (ej. dos altas simultáneas con la misma CURP),
     * sin exponer el detalle SQL.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaApi<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos: {}", ex.getMostSpecificCause().getMessage());
        return construir(CodigoRespuestaEnum.ERROR_CONFLICTO_DATOS,
                "El registro entra en conflicto con información existente (dato único duplicado o referencia inválida)",
                null, HttpStatus.CONFLICT);
    }

    /**
     * Reglas de negocio incumplidas (ej. menor de edad, ingreso no positivo).
     */
    @ExceptionHandler(ValidacionNegocioException.class)
    public ResponseEntity<RespuestaApi<Void>> handleBusinessValidationException(ValidacionNegocioException ex) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_VALIDACION_NEGOCIO, ex.getMessage(), null, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // ─────────────────────────────
    // HTTP - RUTA, MÉTODO Y CONTENIDO
    // ─────────────────────────────

    /**
     * Rutas inexistentes (ej. {@code GET /}).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaApi<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        HttpMethod metodo = ex.getHttpMethod();
        String detalle = String.format("No existe el recurso %s /%s. Consulte la documentación en /swagger-ui.html",
                metodo != null ? metodo.name() : "", ex.getResourcePath());
        return construir(CodigoRespuestaEnum.ERROR_RECURSO_NO_ENCONTRADO, detalle, null, HttpStatus.NOT_FOUND);
    }

    /**
     * Método HTTP no soportado por el recurso.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RespuestaApi<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        String detalle = String.format("El método %s no está permitido para este recurso. Métodos permitidos: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        return construir(CodigoRespuestaEnum.ERROR_METODO_NO_PERMITIDO, detalle, null, HttpStatus.METHOD_NOT_ALLOWED);
    }

    /**
     * {@code Content-Type} distinto de JSON.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<RespuestaApi<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String detalle = ex.getContentType() == null
                ? String.format("La petición no indica el tipo de contenido. Envíe el cuerpo en JSON con 'Content-Type: %s'",
                        MediaType.APPLICATION_JSON_VALUE)
                : String.format("El tipo de contenido '%s' no es soportado. Use 'Content-Type: %s'",
                        ex.getContentType(), MediaType.APPLICATION_JSON_VALUE);
        return construir(CodigoRespuestaEnum.ERROR_TIPO_CONTENIDO, detalle, null, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    // ─────────────────────────────
    // SERVICIOS EXTERNOS
    // ─────────────────────────────

    /**
     * Autenticación rechazada por el servicio externo.
     */
    @ExceptionHandler(ExternalServiceAuthException.class)
    public ResponseEntity<RespuestaApi<Void>> handleExternalServiceAuthException(ExternalServiceAuthException ex) {
        log.error("Fallo de autenticación con el servicio externo: {}", ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_AUTENTICACION, ex.getMessage(), null, HttpStatus.UNAUTHORIZED);
    }

    /**
     * El servicio externo no respondió a tiempo.
     */
    @ExceptionHandler({ExternalServiceTimeoutException.class, RetryableException.class})
    public ResponseEntity<RespuestaApi<Void>> handleTimeoutException(Exception ex) {
        log.error("Timeout de comunicación con servicio externo: {}", ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_TIMEOUT,
                "El servicio externo no respondió dentro del tiempo límite configurado", null, HttpStatus.GATEWAY_TIMEOUT);
    }

    /**
     * Error devuelto por el servicio externo.
     */
    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<RespuestaApi<Void>> handleExternalServiceException(ExternalServiceException ex) {
        log.error("Error devuelto por el servicio externo (status {}): {}", ex.getStatusCode(), ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO, ex.getMessage(), null, HttpStatus.BAD_GATEWAY);
    }

    /**
     * Respuesta no satisfactoria de un cliente Feign.
     */
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<RespuestaApi<Void>> handleFeignException(FeignException ex) {
        log.error("Error de cliente Feign [status {}]: {}", ex.status(), ex.getMessage());
        return construir(CodigoRespuestaEnum.ERROR_SERVICIO_EXTERNO, "Respuesta no satisfactoria del servicio remoto",
                null, HttpStatus.BAD_GATEWAY);
    }

    // ─────────────────────────────
    // 500 - NO CONTROLADO
    // ─────────────────────────────

    /**
     * Cualquier excepción no prevista: se registra completa en el log y al cliente solo se le informa que ocurrió
     * un error interno, sin trazas ni detalles técnicos.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaApi<Void>> handleGenericException(Exception ex) {
        log.error("Error interno no controlado en el servidor: ", ex);
        return construir(CodigoRespuestaEnum.ERROR_INTERNO,
                "Ocurrió un error inesperado al procesar la solicitud; intente más tarde o contacte a soporte técnico",
                null, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ─────────────────────────────
    // AUXILIARES
    // ─────────────────────────────

    private ResponseEntity<RespuestaApi<Void>> construir(CodigoRespuestaEnum codigo, String detalle,
                                                         Map<String, String> errores, HttpStatus status) {
        return new ResponseEntity<>(RespuestaApi.error(codigo, detalle, errores), status);
    }

    private static String mensaje(ObjectError error) {
        return Objects.toString(error.getDefaultMessage(), "Valor inválido");
    }

    private static String ultimoNodo(Path path) {
        String nombre = null;
        for (Path.Node nodo : path) {
            nombre = nodo.getName();
        }
        return nombre != null ? nombre : path.toString();
    }

    private static Throwable excepcionJson(HttpMessageNotReadableException ex) {
        for (Throwable t = ex.getCause(); t != null && t != t.getCause(); t = t.getCause()) {
            if (t instanceof JsonProcessingException) {
                return t;
            }
        }
        return ex;
    }

    private static String rutaJson(MismatchedInputException ex) {
        StringBuilder ruta = new StringBuilder();
        for (JsonMappingException.Reference ref : ex.getPath()) {
            if (ref.getFieldName() != null) {
                if (!ruta.isEmpty()) {
                    ruta.append('.');
                }
                ruta.append(ref.getFieldName());
            } else if (ref.getIndex() >= 0) {
                ruta.append('[').append(ref.getIndex()).append(']');
            }
        }
        return ruta.toString();
    }

    private static String descripcionTipo(Class<?> tipo) {
        if (tipo == null) {
            return "un valor del tipo esperado";
        }
        if (LocalDate.class.isAssignableFrom(tipo)) {
            return "una fecha con formato YYYY-MM-DD";
        }
        if (Temporal.class.isAssignableFrom(tipo)) {
            return "una fecha/hora en formato ISO-8601";
        }
        if (Number.class.isAssignableFrom(tipo) || (tipo.isPrimitive() && tipo != boolean.class)) {
            return "un valor numérico directo (sin comillas)";
        }
        if (Boolean.class == tipo || boolean.class == tipo) {
            return "true o false (sin comillas)";
        }
        if (CharSequence.class.isAssignableFrom(tipo)) {
            return "un texto entre comillas";
        }
        if (tipo.isEnum()) {
            return "uno de los valores permitidos";
        }
        return "un objeto JSON con la estructura documentada";
    }
}
