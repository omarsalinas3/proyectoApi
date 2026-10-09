package com.proyecto.servicios.enums;

import com.proyecto.servicios.exception.ValidacionNegocioException;
import lombok.Getter;

import java.util.Arrays;

/**
 * Catálogo normalizado para el estado civil del cliente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
public enum EstadoCivilEnum {
    SOLTERO("Soltero(a)"),
    CASADO("Casado(a)"),
    UNION_LIBRE("Unión Libre"),
    DIVORCIADO("Divorciado(a)"),
    VIUDO("Viudo(a)");

    private final String descripcion;

    EstadoCivilEnum(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Valida y normaliza la entrada del usuario al valor oficial del catálogo.
     *
     * @param input texto enviado en la petición (ej. "SOLTERO", "SOLTERA", "CASADO", "CASADA", "UNION_LIBRE")
     * @return nombre estándar del catálogo (ej. "SOLTERO", "CASADO", "UNION_LIBRE", "DIVORCIADO", "VIUDO")
     * @throws ValidacionNegocioException si el valor no pertenece al catálogo
     */
    public static String normalizar(String input) {
        if (input == null || input.isBlank()) {
            return SOLTERO.name();
        }
        String clean = input.trim().toUpperCase()
                .replace(" ", "_")
                .replace("Ó", "O")
                .replace("Á", "A");

        return switch (clean) {
            case "SOLTERO", "SOLTERA" -> SOLTERO.name();
            case "CASADO", "CASADA" -> CASADO.name();
            case "UNION_LIBRE", "UNIONLIBRE", "CONCUBINATO" -> UNION_LIBRE.name();
            case "DIVORCIADO", "DIVORCIADA" -> DIVORCIADO.name();
            case "VIUDO", "VIUDA" -> VIUDO.name();
            default -> throw new ValidacionNegocioException(
                    "El estado civil '" + input + "' no es válido. Catálogo permitido: "
                            + Arrays.toString(values()) + " (SOLTERO, CASADO, UNION_LIBRE, DIVORCIADO, VIUDO)"
            );
        };
    }
}
