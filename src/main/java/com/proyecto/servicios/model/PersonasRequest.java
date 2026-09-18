package com.proyecto.servicios.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// CORRECCIÓN: Se eliminó el import 'jakarta.persistence.Column' que no pertenece a una capa DTO.
// CORRECCIÓN: Se agregaron validaciones de Jakarta Validation (@NotBlank) para garantizar consistencia con @Valid en el controller.

/**
 * DTO para la petición de creación y actualización de personas.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonasRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    private String apellidoP;

    private String apellidoMaterno;
}

