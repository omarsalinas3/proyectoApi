package com.proyecto.servicios.model.producto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Modelo JAXB para el elemento individual {@code <producto>} de PuntoRed / GestoPago.
 *
 * <p><strong>IMPORTANTE:</strong> Esta clase NO debe usar {@code @Builder} de Lombok,
 * ya que JAXB requiere instanciar nuevos objetos por cada elemento XML procesado.
 * El uso de {@code @Builder} puede causar reuso del mismo objeto en listas.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.1
 */
@XmlRootElement(name = "producto")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class ProductoXml {

    @XmlAttribute(name = "idProducto")
    private Long idProducto;

    @XmlAttribute(name = "idServicio")
    private Integer idServicio;

    @XmlAttribute(name = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @XmlAttribute(name = "tipoFront")
    private Integer tipoFront;

    @XmlAttribute(name = "producto")
    private String producto;

    @XmlAttribute(name = "servicio")
    private String servicio;

    @XmlAttribute(name = "precio")
    private BigDecimal precio;

    @XmlAttribute(name = "tipoReferencia")
    private String tipoReferencia;

    @XmlAttribute(name = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @XmlAttribute(name = "showAyuda")
    private Boolean showAyuda;

    @XmlElement(name = "legend")
    private String legend;
}
