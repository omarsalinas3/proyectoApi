package com.proyecto.servicios.util;

import java.util.regex.Pattern;

/**
 * Oculta credenciales en textos que van al log.
 *
 * <p>Los errores de Feign incluyen la URL completa de la petición, y GestoPago recibe la contraseña del distribuidor
 * como parámetro de la URL de autenticación ({@code ?password=...}). Sin esto, un fallo de red escribiría la
 * contraseña en el log. También cubre tokens Bearer y el payload cifrado ({@code signed}).</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public final class OcultadorDatosSensibles {

    private static final String OCULTO = "****";
    private static final Pattern PARAMETRO_SENSIBLE = Pattern.compile(
            "(?i)\\b(password|passwd|pwd|token|secret|secret_key|api[-_]?key|signed)=([^&\\s\"']+)");
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9\\-._~+/]+=*");

    private OcultadorDatosSensibles() {
    }

    public static String ocultar(String texto) {
        if (texto == null) {
            return null;
        }
        String resultado = PARAMETRO_SENSIBLE.matcher(texto).replaceAll("$1=" + OCULTO);
        return BEARER.matcher(resultado).replaceAll("Bearer " + OCULTO);
    }

    /** Mensaje del error ya sin credenciales, precedido del tipo de excepción. */
    public static String describir(Throwable error) {
        return error == null ? "" : error.getClass().getSimpleName() + ": " + ocultar(error.getMessage());
    }
}
