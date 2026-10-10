package com.proyecto.servicios.enums;

import lombok.Getter;

/**
 * Enumeración de códigos de respuesta estandarizados para el catálogo de productos.
 *
 * <p><strong>Convención:</strong>
 * <ul>
 *   <li>{@code 0} → Operación exitosa.</li>
 *   <li>Cualquier otro valor → Condición de error o advertencia.</li>
 * </ul>
 * </p>
 *
 * <p><strong>Mapeo HTTP sugerido:</strong>
 * <ul>
 *   <li>{@code EXITO} → {@code 200 OK}</li>
 *   <li>{@code SIN_DATOS_EN_CACHE} → {@code 204 No Content}</li>
 *   <li>{@code ERROR_AUTENTICACION} → {@code 401 Unauthorized}</li>
 *   <li>{@code ERROR_SERVICIO_EXTERNO} → {@code 502 Bad Gateway}</li>
 *   <li>{@code ERROR_TIMEOUT} → {@code 504 Gateway Timeout}</li>
 *   <li>{@code ERROR_INTERNO} → {@code 500 Internal Server Error}</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
public enum CodigoRespuestaEnum {

    // ─────────────────────────────
    //  ÉXITO
    // ─────────────────────────────

    /** Operación completada exitosamente. */
    EXITO(0, "Operación realizada con éxito"),

    // ─────────────────────────────
    //  ADVERTENCIAS / DATOS PARCIALES
    // ─────────────────────────────

    /** No se encontraron datos en caché (Redis ni PostgreSQL). */
    SIN_DATOS_EN_CACHE(1, "No se encontraron datos en caché. Ejecuta GET /api/v1/productos primero."),

    /** Catálogo recuperado desde Redis como respaldo. */
    DATOS_DESDE_CACHE(2, "Catálogo recuperado desde caché Redis"),

    /** Catálogo recuperado desde PostgreSQL como respaldo de último nivel. */
    DATOS_DESDE_BASE_DE_DATOS(3, "Catálogo recuperado desde base de datos local"),

    // ─────────────────────────────
    //  ERRORES
    // ─────────────────────────────

    /** Fallo de autenticación con el servicio externo (token inválido o expirado). */
    ERROR_AUTENTICACION(10, "Error de autenticación con el servicio externo"),

    /** El servicio externo respondió con un error o formato inesperado. */
    ERROR_SERVICIO_EXTERNO(11, "Error al consumir el servicio externo"),

    /** El servicio externo no respondió en el tiempo límite configurado. */
    ERROR_TIMEOUT(12, "Tiempo de espera agotado al conectar con el servicio externo"),

    /** Cliente no encontrado en la base de datos. */
    ERROR_CLIENTE_NO_ENCONTRADO(20, "Cliente no encontrado"),

    /** Cuenta no encontrada en la base de datos. */
    ERROR_CUENTA_NO_ENCONTRADA(21, "Cuenta no encontrada"),

    /** CURP ya registrada en el sistema. */
    ERROR_CURP_DUPLICADA(22, "La CURP ya se encuentra registrada"),

    /** RFC ya registrado en el sistema. */
    ERROR_RFC_DUPLICADO(23, "El RFC ya se encuentra registrado"),

    /** Correo electrónico ya registrado en el sistema. */
    ERROR_CORREO_DUPLICADO(24, "El correo electrónico ya se encuentra registrado"),

    /** Nombre de usuario de login ya registrado. */
    ERROR_USUARIO_DUPLICADO(25, "El nombre de usuario ya se encuentra registrado"),

    /** Credenciales de acceso incorrectas o inválidas. */
    ERROR_CREDENCIALES_INVALIDAS(30, "Credenciales de acceso inválidas"),

    /** Sesión inactiva o expirada por tiempo dinámico. */
    ERROR_SESION_EXPIRADA(31, "La sesión ha expirado por inactividad"),

    /** Violación de regla de negocio o datos inmutables. */
    ERROR_VALIDACION_NEGOCIO(40, "Error en validación de reglas de negocio"),

    /** Error interno del servidor (condición inesperada no controlada). */
    ERROR_INTERNO(99, "Error interno del servidor");

    // ─────────────────────────────────────────────────────────────────────

    /** Valor numérico del código (0 = éxito, mayor a 0 = error/advertencia). */
    private final int codigo;

    /** Descripción legible del resultado. */
    private final String descripcion;

    CodigoRespuestaEnum(int codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    /**
     * Indica si este código representa una operación exitosa.
     *
     * @return {@code true} únicamente si el código es {@code 0}.
     */
    public boolean esExitoso() {
        return this.codigo == 0;
    }

    /**
     * Indica si este código representa una condición de error.
     *
     * @return {@code true} si el código es mayor o igual a {@code 10}.
     */
    public boolean esError() {
        return this.codigo >= 10;
    }

    /**
     * Busca un {@link CodigoRespuestaEnum} a partir de su valor numérico.
     * Si no se encuentra, retorna {@link #ERROR_INTERNO}.
     *
     * @param codigo valor numérico a buscar.
     * @return el enum correspondiente o {@link #ERROR_INTERNO} como fallback.
     */
    public static CodigoRespuestaEnum desdeCodigo(int codigo) {
        for (CodigoRespuestaEnum c : values()) {
            if (c.codigo == codigo) {
                return c;
            }
        }
        return ERROR_INTERNO;
    }
}
