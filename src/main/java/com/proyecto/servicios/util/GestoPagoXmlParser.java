package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

/**
 * Interpreta la respuesta XML de {@code sendTx.do} y {@code confirmTx.do}. El parser se configura sin DTD ni
 * entidades externas para no exponer la aplicación a XXE.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
public class GestoPagoXmlParser {

    private static final int ESTATUS_RESPUESTA_INVALIDA = 502;

    public GestoPagoTxResultado parsear(String xml) {
        if (xml == null || xml.isBlank()) {
            throw new ExternalServiceException("El procesador de pagos devolvió una respuesta vacía", ESTATUS_RESPUESTA_INVALIDA);
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document documento = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.trim().getBytes(StandardCharsets.UTF_8)));
            Element raiz = documento.getDocumentElement();
            Element mensaje = primerHijo(raiz, "MENSAJE");
            String codigo = texto(mensaje, "CODIGO");
            if (codigo == null) {
                throw new ExternalServiceException("La respuesta del procesador de pagos no contiene CODIGO", ESTATUS_RESPUESTA_INVALIDA);
            }
            return GestoPagoTxResultado.builder()
                    .codigo(codigo)
                    .texto(texto(mensaje, "TEXTO"))
                    .referencia(texto(mensaje, "REFERENCIA"))
                    .idTx(entero(primerNoNulo(texto(raiz, "ID_TX"), texto(mensaje, "ID_TX"))))
                    .numAutorizacion(texto(raiz, "NUM_AUTORIZACION"))
                    .saldo(decimal(texto(raiz, "SALDO")))
                    .comision(decimal(texto(raiz, "COMISION")))
                    .monto(decimal(texto(raiz, "MONTO")))
                    .fecha(texto(raiz, "FECHA"))
                    .pin(texto(raiz, "PIN"))
                    .legend(texto(raiz, "legend"))
                    .build();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Respuesta XML de GestoPago no interpretable: {}", e.getMessage());
            throw new ExternalServiceException("El procesador de pagos devolvió una respuesta con formato inválido",
                    e, ESTATUS_RESPUESTA_INVALIDA);
        }
    }

    private static Element primerHijo(Element padre, String nombre) {
        if (padre == null) {
            return null;
        }
        NodeList hijos = padre.getChildNodes();
        for (int i = 0; i < hijos.getLength(); i++) {
            Node hijo = hijos.item(i);
            if (hijo instanceof Element elemento && nombre.equals(elemento.getTagName())) {
                return elemento;
            }
        }
        return null;
    }

    private static String texto(Element padre, String nombre) {
        Element elemento = primerHijo(padre, nombre);
        if (elemento == null) {
            return null;
        }
        String valor = elemento.getTextContent().trim();
        return valor.isEmpty() ? null : valor;
    }

    private static String primerNoNulo(String a, String b) {
        return a != null ? a : b;
    }

    private static Long entero(String valor) {
        try {
            return valor == null ? null : Long.parseLong(valor);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** SALDO puede venir en notación científica (ej. 1.5773183E5): BigDecimal la interpreta. */
    private static BigDecimal decimal(String valor) {
        try {
            return valor == null ? null : new BigDecimal(valor.replace(",", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
