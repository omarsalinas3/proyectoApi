package com.proyecto.servicios.model.producto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenedor JAXB para la lista de productos {@code <PRODUCTOS>}.
 *
 * <p><strong>IMPORTANTE:</strong> Esta clase NO debe usar {@code @Builder} de Lombok.
 * JAXB requiere constructor sin argumentos y acceso directo a campos para la
 * deserialización correcta de listas XML. {@code @Builder.Default} interfiere
 * con la creación de instancias, causando que todos los elementos apunten al mismo objeto.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.1
 */
@XmlRootElement(name = "PRODUCTOS")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class ProductosXml {

    @XmlElement(name = "producto")
    private List<ProductoXml> productos = new ArrayList<>();
}
