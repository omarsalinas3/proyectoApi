package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductosClient;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.service.ProductoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación de la capa de servicio de negocio {@link ProductoService}.
 *
 * <p><strong>Principios aplicados:</strong>
 * <ul>
 *   <li>Inyección de dependencias por constructor.</li>
 *   <li>Soporte y parseo seguro de respuestas tanto en formato XML como JSON con estructuras anidadas de PuntoRed.</li>
 *   <li>Tipado estricto sin tipos varchar/genéricos para datos numéricos y booleanos.</li>
 *   <li>Registro y monitoreo con SLF4J (inicio, duración en ms y total de productos).</li>
 *   <li>Seguridad: Sin exposición de credenciales o tokens en trazas de log.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductosClient productosClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructor para la inyección de dependencias.
     *
     * @param productosClient cliente Feign para la integración externa de productos.
     * @param objectMapper    instancia de {@link ObjectMapper} configurada para JSON.
     */
    public ProductoServiceImpl(ProductosClient productosClient, ObjectMapper objectMapper) {
        this.productosClient = productosClient;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProductoListResponse consultarListaProductos() {
        log.info("Iniciando consumo de servicio externo para obtener lista de productos [GET /sistema/service/getProductList.do]");
        long tiempoInicio = System.currentTimeMillis();

        try {
            // Invocación al cliente Feign de integración
            String rawResponse = productosClient.getProductListRaw();

            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            if (rawResponse == null || rawResponse.isBlank()) {
                log.warn("El servicio externo retornó una respuesta vacía tras {} ms", tiempoTotal);
                return ProductoListResponse.builder()
                        .codigo(204)
                        .mensaje("No se obtuvo contenido del servicio externo")
                        .fechaConsulta(LocalDateTime.now())
                        .build();
            }

            ProductoListResponse response = parsearRespuesta(rawResponse);

            int totalProductos = (response.getProductos() != null) ? response.getProductos().size() : 0;
            log.info("Consumo de servicio externo finalizado exitosamente en {} ms. Total de productos procesados: {}",
                    tiempoTotal, totalProductos);

            if (response.getFechaConsulta() == null) {
                response.setFechaConsulta(LocalDateTime.now());
            }

            return response;

        } catch (Exception ex) {
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;
            log.error("Fallo durante el consumo del servicio externo tras {} ms. Causa: {}", tiempoTotal, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Identifica y parsea la respuesta cruda según su formato (XML o JSON).
     *
     * @param rawBody contenido de respuesta en formato String.
     * @return {@link ProductoListResponse} mapeado con datos fuertemente tipados.
     */
    private ProductoListResponse parsearRespuesta(String rawBody) {
        String trimmed = rawBody.trim();
        if (trimmed.startsWith("<")) {
            return parsearXml(trimmed);
        } else {
            return parsearJson(trimmed);
        }
    }

    /**
     * Parsea la respuesta XML de PuntoRed / GestoPago protegiendo contra vulnerabilidades XXE
     * y sanitizando caracteres especiales no escapados (como '&' en nombres como 'AT&T').
     *
     * @param xml contenido XML recibido del servidor externo.
     * @return {@link ProductoListResponse} con los productos y metadatos extraídos.
     */
    private ProductoListResponse parsearXml(String xml) {
        try {
            String sanitizedXml = sanitizarXml(xml);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            try {
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {
                // Fallback silencioso si no se soportan features específicas
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(sanitizedXml)));
            doc.getDocumentElement().normalize();

            String codigoStr = obtenerTextoElemento(doc, "CODIGO");
            String mensajeStr = obtenerTextoElemento(doc, "TEXTO");
            int codigo = (codigoStr != null && !codigoStr.isBlank()) ? parsearEnteroSeguro(codigoStr) : 200;

            List<ProductoItemDTO> productos = new ArrayList<>();
            NodeList productoNodes = doc.getElementsByTagName("producto");

            for (int i = 0; i < productoNodes.getLength(); i++) {
                Node node = productoNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element el = (Element) node;

                    String legend = null;
                    NodeList legendNodes = el.getElementsByTagName("legend");
                    if (legendNodes.getLength() > 0) {
                        legend = legendNodes.item(0).getTextContent().trim();
                    }

                    ProductoItemDTO item = ProductoItemDTO.builder()
                            .idProducto(parsearLongSeguro(el.getAttribute("idProducto")))
                            .idServicio(parsearEnteroSeguro(el.getAttribute("idServicio")))
                            .idCatTipoServicio(parsearEnteroSeguro(el.getAttribute("idCatTipoServicio")))
                            .tipoFront(parsearEnteroSeguro(el.getAttribute("tipoFront")))
                            .producto(el.getAttribute("producto"))
                            .servicio(el.getAttribute("servicio"))
                            .precio(parsearBigDecimalSeguro(el.getAttribute("precio")))
                            .tipoReferencia(el.getAttribute("tipoReferencia"))
                            .hasDigitoVerificador(parsearBooleanoSeguro(el.getAttribute("hasDigitoVerificador")))
                            .showAyuda(parsearBooleanoSeguro(el.getAttribute("showAyuda")))
                            .legend(legend)
                            .build();

                    productos.add(item);
                }
            }

            return ProductoListResponse.builder()
                    .codigo(codigo)
                    .mensaje(mensajeStr != null && !mensajeStr.isBlank() ? mensajeStr : "Operacion realizada con exito")
                    .totalRegistros((long) productos.size())
                    .fechaConsulta(LocalDateTime.now())
                    .productos(productos)
                    .build();

        } catch (Exception ex) {
            log.error("Error al procesar la respuesta XML del servicio externo: {}", ex.getMessage());
            throw new ExternalServiceException("Error al procesar el formato de respuesta del servicio remoto", 502);
        }
    }

    /**
     * Parsea la respuesta en formato JSON de forma resiliente ante estructuras anidadas de PuntoRed.
     *
     * @param json contenido JSON recibido.
     * @return {@link ProductoListResponse} mapeado.
     */
    private ProductoListResponse parsearJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // Validar si el servicio remoto retornó error de token expirado
            if (root.has("token") && "EXPIRED".equalsIgnoreCase(root.get("token").asText())) {
                String errorMsg = root.has("message") ? root.get("message").asText() : "El token de autenticación ha expirado";
                log.warn("El servicio remoto indicó token expirado: {}", errorMsg);
                throw new ExternalServiceAuthException("El token de autenticación con el servicio remoto ha expirado. " + errorMsg, 401);
            }

            // Validar si el servicio remoto retornó estado fallido explícito sin productos
            if (root.has("success") && !root.get("success").asBoolean() && !root.has("productos") && !root.has("PRODUCTOS")) {
                String errorMsg = root.has("message") ? root.get("message").asText() : "El servicio remoto retornó un estado fallido";
                log.warn("El servicio remoto retornó respuesta no exitosa: {}", errorMsg);
                throw new ExternalServiceException("El servicio remoto retornó respuesta no exitosa: " + errorMsg, 502);
            }

            // Extraer código
            int codigo = 200;
            if (root.has("codigo") && root.get("codigo").isNumber()) {
                codigo = root.get("codigo").asInt();
            } else if (root.has("CODIGO")) {
                codigo = parsearEnteroSeguro(root.get("CODIGO").asText());
            }

            // Extraer mensaje (puede venir como String o como objeto anidado {CODIGO, TEXTO})
            String mensaje = "Operacion realizada con exito";
            if (root.has("mensaje")) {
                JsonNode msgNode = root.get("mensaje");
                if (msgNode.isObject()) {
                    if (msgNode.has("texto")) mensaje = msgNode.get("texto").asText();
                    else if (msgNode.has("TEXTO")) mensaje = msgNode.get("TEXTO").asText();
                    if (msgNode.has("codigo")) codigo = parsearEnteroSeguro(msgNode.get("codigo").asText());
                    else if (msgNode.has("CODIGO")) codigo = parsearEnteroSeguro(msgNode.get("CODIGO").asText());
                } else {
                    mensaje = msgNode.asText();
                }
            } else if (root.has("MENSAJE")) {
                JsonNode msgNode = root.get("MENSAJE");
                if (msgNode.isObject()) {
                    if (msgNode.has("TEXTO")) mensaje = msgNode.get("TEXTO").asText();
                    else if (msgNode.has("texto")) mensaje = msgNode.get("texto").asText();
                    if (msgNode.has("CODIGO")) codigo = parsearEnteroSeguro(msgNode.get("CODIGO").asText());
                    else if (msgNode.has("codigo")) codigo = parsearEnteroSeguro(msgNode.get("codigo").asText());
                } else {
                    mensaje = msgNode.asText();
                }
            } else if (root.has("message")) {
                mensaje = root.get("message").asText();
            }

            // Extraer lista de productos
            List<ProductoItemDTO> productos = new ArrayList<>();
            JsonNode prodContainer = null;
            if (root.has("productos")) prodContainer = root.get("productos");
            else if (root.has("PRODUCTOS")) prodContainer = root.get("PRODUCTOS");

            if (prodContainer != null) {
                if (prodContainer.isArray()) {
                    for (JsonNode itemNode : prodContainer) {
                        productos.add(parsearItemJson(itemNode));
                    }
                } else if (prodContainer.isObject()) {
                    JsonNode arrayOrObj = prodContainer.has("producto") ? prodContainer.get("producto") :
                            (prodContainer.has("PRODUCTO") ? prodContainer.get("PRODUCTO") : null);

                    if (arrayOrObj != null) {
                        if (arrayOrObj.isArray()) {
                            for (JsonNode itemNode : arrayOrObj) {
                                productos.add(parsearItemJson(itemNode));
                            }
                        } else if (arrayOrObj.isObject()) {
                            productos.add(parsearItemJson(arrayOrObj));
                        }
                    }
                }
            }

            return ProductoListResponse.builder()
                    .codigo(codigo)
                    .mensaje(mensaje)
                    .totalRegistros((long) productos.size())
                    .fechaConsulta(LocalDateTime.now())
                    .productos(productos)
                    .build();

        } catch (ExternalServiceAuthException | ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al procesar la respuesta JSON del servicio externo: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("Error al procesar el formato de respuesta del servicio remoto", 502);
        }
    }

    private ProductoItemDTO parsearItemJson(JsonNode node) {
        JsonNode attrs = node.has("$") ? node.get("$") : node;

        Long idProducto = parsearLongSeguro(obtenerTextoJson(attrs, "idProducto", "ID_PRODUCTO", "idproducto"));
        Integer idServicio = parsearEnteroSeguro(obtenerTextoJson(attrs, "idServicio", "ID_SERVICIO", "idservicio"));
        Integer idCatTipoServicio = parsearEnteroSeguro(obtenerTextoJson(attrs, "idCatTipoServicio", "ID_CAT_TIPO_SERVICIO", "idcattiposervicio"));
        Integer tipoFront = parsearEnteroSeguro(obtenerTextoJson(attrs, "tipoFront", "TIPO_FRONT", "tipofront"));
        String producto = obtenerTextoJson(attrs, "producto", "PRODUCTO");
        String servicio = obtenerTextoJson(attrs, "servicio", "SERVICIO");
        BigDecimal precio = parsearBigDecimalSeguro(obtenerTextoJson(attrs, "precio", "PRECIO"));
        String tipoReferencia = obtenerTextoJson(attrs, "tipoReferencia", "TIPO_REFERENCIA", "tiporeferencia");
        Boolean hasDigitoVerificador = parsearBooleanoSeguro(obtenerTextoJson(attrs, "hasDigitoVerificador", "HAS_DIGITO_VERIFICADOR", "hasdigitoverificador"));
        Boolean showAyuda = parsearBooleanoSeguro(obtenerTextoJson(attrs, "showAyuda", "SHOW_AYUDA", "showayuda"));

        String legend = null;
        if (node.has("legend")) legend = node.get("legend").asText();
        else if (node.has("LEGEND")) legend = node.get("LEGEND").asText();

        return ProductoItemDTO.builder()
                .idProducto(idProducto)
                .idServicio(idServicio)
                .idCatTipoServicio(idCatTipoServicio)
                .tipoFront(tipoFront)
                .producto(producto)
                .servicio(servicio)
                .precio(precio)
                .tipoReferencia(tipoReferencia)
                .hasDigitoVerificador(hasDigitoVerificador)
                .showAyuda(showAyuda)
                .legend(legend)
                .build();
    }

    private String obtenerTextoJson(JsonNode node, String... keys) {
        if (node == null || !node.isObject()) return null;
        for (String key : keys) {
            if (node.has(key)) {
                return node.get(key).asText();
            }
        }
        java.util.Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            for (String key : keys) {
                if (fieldName.equalsIgnoreCase(key) || fieldName.replace("_", "").equalsIgnoreCase(key.replace("_", ""))) {
                    return node.get(fieldName).asText();
                }
            }
        }
        return null;
    }

    private String sanitizarXml(String xml) {
        if (xml == null) return "";
        return xml.replaceAll("&(?!(amp|lt|gt|quot|apos|#\\d+|#x[0-9a-fA-F]+);)", "&amp;");
    }

    private String obtenerTextoElemento(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0).getTextContent().trim();
        }
        return null;
    }

    private Long parsearLongSeguro(String valor) {
        try {
            return (valor != null && !valor.isBlank()) ? Long.valueOf(valor.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer parsearEnteroSeguro(String valor) {
        try {
            return (valor != null && !valor.isBlank()) ? Integer.valueOf(valor.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parsearBigDecimalSeguro(String valor) {
        try {
            return (valor != null && !valor.isBlank()) ? new BigDecimal(valor.trim()) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Boolean parsearBooleanoSeguro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return Boolean.valueOf(valor.trim());
    }
}
