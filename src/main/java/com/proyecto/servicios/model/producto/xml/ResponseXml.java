package com.proyecto.servicios.model.producto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Elemento raíz JAXB {@code <RESPONSE>} de la respuesta XML de PuntoRed / GestoPago.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@XmlRootElement(name = "RESPONSE")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class ResponseXml {

    @XmlElement(name = "MENSAJE")
    private MensajeXml mensaje;

    @XmlElement(name = "PRODUCTOS")
    private ProductosXml productos;
}
