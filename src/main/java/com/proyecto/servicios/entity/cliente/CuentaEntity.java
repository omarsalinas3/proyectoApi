package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA principal para la Cuenta Bancaria asignada a un Cliente (tabla {@code cuenta}).
 *
 * <p><strong>Reglas de negocio:</strong>
 * <ul>
 *   <li>{@code numeroCuenta} de exactamente 16 dígitos irrepetible y único.</li>
 *   <li>Saldo inicial configurado por defecto en $0.00.</li>
 *   <li>Estatus inicial {@code ACTIVA}.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "cuenta")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @Column(name = "numero_cuenta", nullable = false, unique = true, columnDefinition = "TEXT")
    private String numeroCuenta;

    @Column(name = "saldo_inicial", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoDisponible = BigDecimal.ZERO;

    @Column(name = "estatus", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String estatus = "ACTIVA";

    @CreationTimestamp
    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private OffsetDateTime fechaApertura;

    @UpdateTimestamp
    @Column(name = "fecha_modificacion", nullable = false)
    private OffsetDateTime fechaModificacion;

    @OneToOne(mappedBy = "cuenta", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private SaldoEntity saldo;
}
