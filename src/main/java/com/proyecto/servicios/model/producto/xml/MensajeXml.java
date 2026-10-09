package com.proyecto.servicios.model.producto.xml;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modelo JAXB para el elemento {@code <MENSAJE>} de PuntoRed / GestoPago.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@XmlRootElement(name = "MENSAJE")
@XmlAccessorType(XmlAccessType.FIELD)
@Getter
@Setter
@NoArgsConstructor
public class MensajeXml {

    @XmlElement(name = "CODIGO")
    private Integer codigo;

    @XmlElement(name = "TEXTO")
    private String texto;
}
