package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rechaza peticiones que incluyan campos que no forman parte del contrato del Request DTO, por ejemplo
 * campos controlados por el servidor (estatus, CURP/RFC en una actualización, número de cuenta, saldos).
 * Se aplica sobre el mapa donde {@code RequestEstricto} acumula las propiedades desconocidas.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Documented
@Constraint(validatedBy = SinCamposNoPermitidosValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SinCamposNoPermitidos {

    String message() default "La petición contiene campos no permitidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
