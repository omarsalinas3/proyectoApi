package com.proyecto.servicios.enums;

/**
 * Roles de acceso a la API.
 *
 * <ul>
 *   <li>{@code CLIENTE}: cliente persona física; solo puede consultar y modificar su propia información.</li>
 *   <li>{@code EJECUTIVO}: ejecutivo de la institución; puede consultar y administrar a todos los clientes.</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public enum RolEnum {
    CLIENTE,
    EJECUTIVO;

    /**
     * Nombre de la autoridad de Spring Security asociada al rol (ej. {@code ROLE_EJECUTIVO}).
     */
    public String authority() {
        return "ROLE_" + name();
    }
}
