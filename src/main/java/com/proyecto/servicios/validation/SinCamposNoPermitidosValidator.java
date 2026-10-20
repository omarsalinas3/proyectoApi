package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Map;

/**
 * Genera un mensaje que enumera exactamente los campos que el cliente debe quitar de la petición.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public class SinCamposNoPermitidosValidator implements ConstraintValidator<SinCamposNoPermitidos, Map<String, Object>> {

    @Override
    public boolean isValid(Map<String, Object> camposNoPermitidos, ConstraintValidatorContext context) {
        if (camposNoPermitidos == null || camposNoPermitidos.isEmpty()) {
            return true;
        }
        // Se escapan los caracteres especiales de las plantillas de Bean Validation para que el nombre
        // recibido se muestre tal cual y no se interprete como expresión.
        String campos = String.join(", ", camposNoPermitidos.keySet())
                .replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}").replace("$", "\\$");
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                        "No se permiten los campos: " + campos
                                + ". Son controlados por el servidor o no existen en esta operación")
                .addConstraintViolation();
        return false;
    }
}
