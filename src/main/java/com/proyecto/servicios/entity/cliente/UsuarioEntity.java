package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa a un Cliente Persona Física (tabla {@code usuarios}).
 *
 * <p><strong>Reglas de diseño:</strong>
 * <ul>
 *   <li>Todos los campos de texto se almacenan como {@code TEXT} en PostgreSQL (definidos con columnDefinition = "TEXT").</li>
 *   <li>Baja lógica gestionada mediante el atributo {@code activo}.</li>
 *   <li>Unicidad estricta en {@code curp}, {@code rfc} y {@code correo}.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, columnDefinition = "TEXT")
    private String nombre;

    @Column(name = "segundo_nombre", columnDefinition = "TEXT")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false, columnDefinition = "TEXT")
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false, columnDefinition = "TEXT")
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(name = "curp", nullable = false, unique = true, columnDefinition = "TEXT")
    private String curp;

    @Column(name = "rfc", nullable = false, unique = true, columnDefinition = "TEXT")
    private String rfc;

    @Column(name = "sexo", nullable = false, columnDefinition = "TEXT")
    private String sexo;

    @Column(name = "nacionalidad", nullable = false, columnDefinition = "TEXT")
    private String nacionalidad;

    @Column(name = "estado_civil", nullable = false, columnDefinition = "TEXT")
    private String estadoCivil;

    @Column(name = "correo", nullable = false, unique = true, columnDefinition = "TEXT")
    private String correo;

    @Column(name = "telefono_movil", nullable = false, columnDefinition = "TEXT")
    private String telefonoMovil;

    @Column(name = "telefono_alternativo", columnDefinition = "TEXT")
    private String telefonoAlternativo;

    @Column(name = "ocupacion", nullable = false, columnDefinition = "TEXT")
    private String ocupacion;

    @Column(name = "empresa", nullable = false, columnDefinition = "TEXT")
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresoMensual;

    @Column(name = "ine_documento_url", columnDefinition = "TEXT")
    private String ineDocumentoUrl;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private OffsetDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private DomicilioEntity domicilio;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private CuentaEntity cuenta;

    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private LoginEntity login;
}
