package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

// CORRECCIÓN: Se cambió el atributo 'private Integer Id' por 'private Integer id' para respetar las convenciones de nomenclatura Java y JavaBeans.

/**
 * Entidad JPA para la tabla 'personas'.
 */
@Table(name="personas")
@Entity
@Getter
@Setter
public class Personas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name="nombre")
    private String nombre;

    @Column(name="apellido_paterno")
    private String apellidoP;

    @Column(name="apellido_materno")
    private String apellidoMaterno;
}
