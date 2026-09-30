package com.proyecto.servicios.entity.cliente;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidad JPA que representa la información de saldos de la cuenta de un usuario (tabla {@code saldos}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "saldos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaldoEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false, unique = true)
    private CuentaEntity cuenta;

    @Column(name = "saldo_actual", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoActual = BigDecimal.ZERO;

    @Column(name = "saldo_retenido", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoRetenido = BigDecimal.ZERO;

    @Column(name = "saldo_disponible", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal saldoDisponible = BigDecimal.ZERO;

    @Column(name = "moneda", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String moneda = "MXN";

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
