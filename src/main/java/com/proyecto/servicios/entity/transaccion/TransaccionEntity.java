package com.proyecto.servicios.entity.transaccion;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.EstatusTransaccionEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Transacción de venta/pago enviada a GestoPago ({@code sendTx}) con su evidencia de petición y respuesta.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "transacciones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaEntity cuenta;

    /** Identificador único de la operación (UPC) que GestoPago usa para detectar duplicados. */
    @Column(name = "upc", nullable = false, unique = true, length = 15)
    private String upc;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "telefono", nullable = false, length = 20)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "monto", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "hora_local", nullable = false, length = 14)
    private String horaLocal;

    @Column(name = "unidad", nullable = false, length = 25)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 25)
    private EstatusTransaccionEnum estatus;

    @Column(name = "codigo_respuesta", length = 4)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", length = 600)
    private String mensajeRespuesta;

    @Column(name = "id_tx_gestopago")
    private Long idTxGestoPago;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "comision", precision = 15, scale = 2)
    private BigDecimal comision;

    /** JSON en claro enviado a GestoPago (evidencia obligatoria; se reutiliza tal cual en confirmTx). */
    @Column(name = "payload_enviado", nullable = false, columnDefinition = "TEXT")
    private String payloadEnviado;

    /** Valor del parámetro {@code signed}: el payload cifrado con AES-256-CBC. */
    @Column(name = "peticion_cifrada", columnDefinition = "TEXT")
    private String peticionCifrada;

    /** Respuesta XML recibida de GestoPago (evidencia obligatoria). */
    @Column(name = "respuesta_gestopago", columnDefinition = "TEXT")
    private String respuestaGestoPago;

    @Column(name = "fecha_envio")
    private OffsetDateTime fechaEnvio;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion", nullable = false)
    private OffsetDateTime fechaActualizacion;
}
