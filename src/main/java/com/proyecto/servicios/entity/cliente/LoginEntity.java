package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que gestiona las credenciales de acceso, biometría de alta precisión
 * y control de sesión activa/inactiva (tabla {@code login}).
 *
 * <p><strong>Reglas de seguridad y diseño:</strong>
 * <ul>
 *   <li>Username, password y datos biométricos se persisten cifrados.</li>
 *   <li>Mapeo de datos biométricos a nivel de Java mediante {@link BigDecimal} para máxima exactitud numérica.</li>
 *   <li>Control de sesión mediante bandera booleana {@code sesionActiva} y contador de inactividad (default 300 segundos).</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "login")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private UsuarioEntity usuario;

    @Column(name = "username_cifrado", nullable = false, unique = true, columnDefinition = "TEXT")
    private String usernameCifrado;

    @Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
    private String passwordHash;

    /**
     * Mapeo de datos biométricos de alta precisión usando BigDecimal en Java.
     */
    @Column(name = "datos_biometricos_precision")
    private BigDecimal datosBiometricosPrecision;

    /**
     * Datos biométricos serializados y cifrados para almacenamiento seguro.
     */
    @Column(name = "datos_biometricos_cifrados", columnDefinition = "TEXT")
    private String datosBiometricosCifrados;

    @Column(name = "sesion_activa", nullable = false)
    @Builder.Default
    private Boolean sesionActiva = false;

    @Column(name = "tiempo_inactividad_segundos", nullable = false)
    @Builder.Default
    private Integer tiempoInactividadSegundos = 300;

    @Column(name = "ultima_actividad")
    private OffsetDateTime ultimaActividad;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
