package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa el Domicilio asociado a un Cliente Persona Física (tabla {@code domicilios}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "domicilios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomicilioEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(name = "calle", nullable = false, columnDefinition = "TEXT")
    private String calle;

    @Column(name = "numero_exterior", nullable = false, columnDefinition = "TEXT")
    private String numeroExterior;

    @Column(name = "numero_interior", columnDefinition = "TEXT")
    private String numeroInterior;

    @Column(name = "colonia", nullable = false, columnDefinition = "TEXT")
    private String colonia;

    @Column(name = "municipio", nullable = false, columnDefinition = "TEXT")
    private String municipio;

    @Column(name = "estado", nullable = false, columnDefinition = "TEXT")
    private String estado;

    @Column(name = "codigo_postal", nullable = false, columnDefinition = "TEXT")
    private String codigoPostal;

    @Column(name = "pais", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String pais = "México";

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;
}
