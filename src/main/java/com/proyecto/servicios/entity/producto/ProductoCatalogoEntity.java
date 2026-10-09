package com.proyecto.servicios.entity.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad JPA representativa de la tabla {@code productos_catalogo} en PostgreSQL.
 *
 * <p>Almacena el catálogo respaldado de productos para operar en modo offline/fallback
 * cuando el servicio remoto de PuntoRed / GestoPago o Redis no se encuentren disponibles.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Entity
@Table(name = "productos_catalogo")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoCatalogoEntity {

    @Id
    @Column(name = "id_producto", nullable = false)
    private Long idProducto;

    @Column(name = "id_servicio")
    private Integer idServicio;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "tipo_front")
    private Integer tipoFront;

    @Column(name = "producto")
    private String producto;

    @Column(name = "servicio")
    private String servicio;

    @Column(name = "precio", precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(name = "tipo_referencia", length = 50)
    private String tipoReferencia;

    @Column(name = "has_digito_verificador")
    private Boolean hasDigitoVerificador;

    @Column(name = "show_ayuda")
    private Boolean showAyuda;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
}
