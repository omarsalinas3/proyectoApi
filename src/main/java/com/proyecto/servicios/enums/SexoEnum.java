package com.proyecto.servicios.enums;

import com.proyecto.servicios.exception.ValidacionNegocioException;
import lombok.Getter;

import java.util.Arrays;

/**
 * Catálogo normalizado para el género/sexo del cliente.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Getter
public enum SexoEnum {
    M("Masculino"),
    F("Femenino"),
    OTRO("Otro");

    private final String descripcion;

    SexoEnum(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Valida y normaliza la entrada del usuario al valor del catálogo.
     *
     * @param input texto enviado en la petición (ej. "M", "F", "MASCULINO", "FEMENINO")
     * @return representación estándar ("M", "F", "OTRO")
     * @throws ValidacionNegocioException si el valor no pertenece al catálogo
     */
    public static String normalizar(String input) {
        if (input == null || input.isBlank()) {
            return M.name();
        }
        String clean = input.trim().toUpperCase();
        return switch (clean) {
            case "M", "MASCULINO", "HOMBRE", "H" -> M.name();
            case "F", "FEMENINO", "MUJER" -> F.name();
            case "OTRO", "NO_BINARIO", "NB" -> OTRO.name();
            default -> throw new ValidacionNegocioException(
                    "El valor de sexo '" + input + "' no es válido. Catálogo permitido: "
                            + Arrays.toString(values()) + " (o MASCULINO, FEMENINO)"
            );
        };
    }
}
