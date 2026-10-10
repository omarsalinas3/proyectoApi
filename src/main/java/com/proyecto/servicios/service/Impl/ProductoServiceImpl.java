package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.client.ProductosClient;
import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.ExternalServiceAuthException;
import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.exception.ExternalServiceTimeoutException;
import com.proyecto.servicios.mapper.ProductoMapper;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.request.ProductoConsultaRequest;
import com.proyecto.servicios.model.producto.response.ProductoAgrupadoResponse;
import com.proyecto.servicios.model.producto.response.ProductoListResponse;
import com.proyecto.servicios.model.producto.xml.ResponseXml;
import com.proyecto.servicios.repositorys.producto.ProductoCatalogoRepository;
import com.proyecto.servicios.service.ProductoService;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.ValidationEventLocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Implementación robusta del servicio de productos integrando:
 * <ul>
 *   <li>Parseo con JAXB y diagnóstico de línea exacta ante errores XML.</li>
 *   <li>Mapeo estructurado de datos vía MapStruct.</li>
 *   <li>Estrategia de resiliencia escalonada: API ➔ Redis ➔ PostgreSQL.</li>
 *   <li>Procesamiento funcional y agrupación por {@code tipoFront} con {@link Stream} y {@link Predicate}.</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Service
@Slf4j
public class ProductoServiceImpl implements ProductoService {

    private final ProductosClient productosClient;
    private final ObjectMapper objectMapper;
    private final ProductoMapper productoMapper;
    private final ProductoCatalogoRepository productoCatalogoRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${catalogo.redis.key:catalogo:productos}")
    private String redisCatalogKey;

    @Value("${catalogo.redis.ttl-hours:24}")
    private long redisTtlHours;

    @Autowired
    public ProductoServiceImpl(
            ProductosClient productosClient,
            ObjectMapper objectMapper,
            ProductoMapper productoMapper,
            @Autowired(required = false) ProductoCatalogoRepository productoCatalogoRepository,
            @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.productosClient = productosClient;
        this.objectMapper = objectMapper;
        this.productoMapper = productoMapper;
        this.productoCatalogoRepository = productoCatalogoRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Predicado funcional para evaluar si un producto corresponde a un {@code tipoFront} determinado.
     *
     * @param tipoFront identificador de presentación esperado.
     * @return {@link Predicate} evaluador.
     */
    public static Predicate<ProductoItemDTO> esTipoFront(Integer tipoFront) {
        return p -> p != null && Objects.equals(p.getTipoFront(), tipoFront);
    }

    public static final Predicate<ProductoItemDTO> esTipoFront1 = esTipoFront(1);
    public static final Predicate<ProductoItemDTO> esTipoFront2 = esTipoFront(2);
    public static final Predicate<ProductoItemDTO> esTipoFront3 = esTipoFront(3);

    @Override
    public ProductoListResponse consultarListaProductos() {
        long tiempoInicio = System.currentTimeMillis();
        log.info("Iniciando consulta resiliente de catálogo de productos");

        try {
            // 1. Intentar consumir el servicio remoto externo
            String rawResponse = productosClient.getProductListRaw();
            long tiempoTotal = System.currentTimeMillis() - tiempoInicio;

            if (rawResponse == null || rawResponse.isBlank()) {
                log.warn("El servicio remoto retornó respuesta vacía tras {} ms. Evaluando fallback...", tiempoTotal);
                return obtenerDesdeFallback("Servicio remoto retornó contenido vacío");
            }

            // 2. Parsear respuesta (JAXB para XML o Jackson para JSON)
            ProductoListResponse response = parsearRespuesta(rawResponse);
            response.setOrigenDatos("SERVICIO_EXTERNO");
            if (response.getFechaConsulta() == null) {
                response.setFechaConsulta(LocalDateTime.now());
            }

            log.info("Consumo de servicio externo exitoso en {} ms. Total de productos: {}",
                    tiempoTotal, (response.getProductos() != null ? response.getProductos().size() : 0));

            // 3. Persistir en Redis y en PostgreSQL en modo resiliente
            guardarEnRedis(response);
            guardarEnBaseDeDatos(response.getProductos());

            return response;

        } catch (ExternalServiceAuthException ex) {
            log.error("Fallo de autenticación con el servicio remoto: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.warn("Fallo al consultar servicio remoto ({}: {}). Activando fallback escalonado...",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return obtenerDesdeFallback(ex.getMessage(), ex);
        }
    }

    @Override
    public ProductoAgrupadoResponse obtenerProductosAgrupadosPorTipoFront() {
        ProductoListResponse catalogo = consultarListaProductos();
        List<ProductoItemDTO> productos = (catalogo.getProductos() != null) ? catalogo.getProductos() : Collections.emptyList();

        // 1. Filtrado funcional usando Streams y Predicates específicos
        List<ProductoItemDTO> grupoFront1 = productos.stream().filter(esTipoFront1).collect(Collectors.toList());
        List<ProductoItemDTO> grupoFront2 = productos.stream().filter(esTipoFront2).collect(Collectors.toList());
        List<ProductoItemDTO> grupoFront3 = productos.stream().filter(esTipoFront3).collect(Collectors.toList());

        // 2. Agrupación global con groupingBy sobre Stream
        Map<Integer, List<ProductoItemDTO>> grupos = productos.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        p -> p.getTipoFront() != null ? p.getTipoFront() : 0,
                        Collectors.toList()
                ));

        // Asegurar que las llaves 1, 2 y 3 siempre existan en el mapa de salida
        grupos.putIfAbsent(1, grupoFront1);
        grupos.putIfAbsent(2, grupoFront2);
        grupos.putIfAbsent(3, grupoFront3);

        // El codigoRespuesta del agrupado depende del origen del catálogo consultado
        CodigoRespuestaEnum codigoRespuesta = (catalogo.getCodigoRespuesta() != null)
                ? catalogo.getCodigoRespuesta()
                : CodigoRespuestaEnum.EXITO;

        return ProductoAgrupadoResponse.builder()
                .codigoRespuesta(codigoRespuesta)
                .codigo(codigoRespuesta.getCodigo())
                .mensaje("Productos agrupados exitosamente por tipoFront")
                .origenDatos(catalogo.getOrigenDatos())
                .totalRegistros((long) productos.size())
                .fechaConsulta(LocalDateTime.now())
                .grupos(grupos)
                .build();
    }

    /**
     * Identifica y parsea la respuesta cruda según su formato (XML con JAXB o JSON con Jackson).
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
     * Parsea la respuesta XML de PuntoRed / GestoPago utilizando JAXB,
     * detectando la línea y columna exacta ante cualquier error de sintaxis y preservando la causa raíz.
     */
    private ProductoListResponse parsearXml(String xml) {
        StringBuilder errorUbicacion = new StringBuilder();
        try {
            String sanitizedXml = sanitizarXml(xml);
            JAXBContext context = JAXBContext.newInstance(ResponseXml.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();

            unmarshaller.setEventHandler((ValidationEvent event) -> {
                if (event.getSeverity() >= ValidationEvent.WARNING) {
                    ValidationEventLocator locator = event.getLocator();
                    int linea = (locator != null) ? locator.getLineNumber() : -1;
                    int columna = (locator != null) ? locator.getColumnNumber() : -1;
                    errorUbicacion.append(String.format("[Línea %d, Columna %d: %s] ", linea, columna, event.getMessage()));
                }
                return true;
            });

            ResponseXml responseXml = (ResponseXml) unmarshaller.unmarshal(new StringReader(sanitizedXml));

            if (responseXml == null) {
                throw new ExternalServiceException("El contenido XML deserializado resultó nulo", 502);
            }

            int codigo = 200;
            String mensaje = "Operacion realizada con exito";
            if (responseXml.getMensaje() != null) {
                if (responseXml.getMensaje().getCodigo() != null) {
                    codigo = responseXml.getMensaje().getCodigo();
                }
                if (responseXml.getMensaje().getTexto() != null && !responseXml.getMensaje().getTexto().isBlank()) {
                    mensaje = responseXml.getMensaje().getTexto();
                }
            }

            List<ProductoItemDTO> productos = Collections.emptyList();
            if (responseXml.getProductos() != null && responseXml.getProductos().getProductos() != null) {
                productos = productoMapper.xmlListToDtoList(responseXml.getProductos().getProductos());
            }

            return ProductoListResponse.builder()
                    .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                    .codigo(CodigoRespuestaEnum.EXITO.getCodigo())
                    .mensaje(mensaje)
                    .totalRegistros((long) productos.size())
                    .fechaConsulta(LocalDateTime.now())
                    .origenDatos("SERVICIO_EXTERNO")
                    .productos(productos)
                    .build();

        } catch (JAXBException ex) {
            String detalle = errorUbicacion.length() > 0 ? " Ubicación del error: " + errorUbicacion : "";
            log.error("Error al procesar XML con JAXB.{}: {}", detalle, ex.getMessage(), ex);
            throw new ExternalServiceException("Error en formato XML del servicio remoto." + detalle, ex, 502);
        } catch (Exception ex) {
            log.error("Error inesperado al parsear XML: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("Error inesperado al procesar XML del servicio remoto: " + ex.getMessage(), ex, 502);
        }
    }

    /**
     * Parsea la respuesta en formato JSON de forma resiliente ante variantes de claves.
     */
    private ProductoListResponse parsearJson(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);

            // Validar token expirado
            if (root.has("token") && "EXPIRED".equalsIgnoreCase(root.get("token").asText())) {
                String errorMsg = root.has("message") ? root.get("message").asText() : "El token de autenticación ha expirado";
                log.warn("Servicio remoto indicó token expirado: {}", errorMsg);
                throw new ExternalServiceAuthException("El token de autenticación con el servicio remoto ha expirado. " + errorMsg, 401);
            }

            // Validar estado fallido explícito sin productos
            if (root.has("success") && !root.get("success").asBoolean() && !root.has("productos") && !root.has("PRODUCTOS")) {
                String errorMsg = root.has("message") ? root.get("message").asText() : "El servicio remoto retornó estado no exitoso";
                log.warn("Servicio remoto retornó respuesta no exitosa: {}", errorMsg);
                throw new ExternalServiceException("El servicio remoto retornó respuesta no exitosa: " + errorMsg, 502);
            }

            int codigo = 200;
            if (root.has("codigo") && root.get("codigo").isNumber()) {
                codigo = root.get("codigo").asInt();
            } else if (root.has("CODIGO")) {
                codigo = parsearEnteroSeguro(root.get("CODIGO").asText());
            }

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
                    .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                    .codigo(CodigoRespuestaEnum.EXITO.getCodigo())
                    .mensaje(mensaje)
                    .totalRegistros((long) productos.size())
                    .fechaConsulta(LocalDateTime.now())
                    .origenDatos("SERVICIO_EXTERNO")
                    .productos(productos)
                    .build();

        } catch (ExternalServiceAuthException | ExternalServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Error al procesar JSON con Jackson: {}", ex.getMessage(), ex);
            throw new ExternalServiceException("Error al procesar el formato de respuesta del servicio remoto", ex, 502);
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
        Iterator<String> fieldNames = node.fieldNames();
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

    // ===================================================================
    // CONSULTA DESDE CACHÉ (POST: REDIS → POSTGRESQL)
    // ===================================================================

    @Override
    public ProductoListResponse consultarDesdeCacheOBaseDeDatos(ProductoConsultaRequest request) {
        log.info("[POST] Iniciando consulta de catálogo exclusivamente desde caché (Redis → PostgreSQL)");

        // 1. Intentar leer desde Redis (primera capa de caché)
        ProductoListResponse resultado = leerDesdeRedis();

        // 2. Si Redis no tiene datos, consultar PostgreSQL (segunda capa)
        if (resultado == null) {
            log.info("[POST] Redis vacío o no disponible. Consultando PostgreSQL como segunda capa...");
            resultado = leerDesdeBaseDeDatos();
        }

        // 3. Si ambas fuentes están vacías, devolver respuesta vacía
        if (resultado == null) {
            log.warn("[POST] No se encontraron datos en Redis ni en PostgreSQL. Devolviendo catálogo vacío.");
            return ProductoListResponse.builder()
                    .codigoRespuesta(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE)
                    .codigo(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE.getCodigo())
                    .mensaje(CodigoRespuestaEnum.SIN_DATOS_EN_CACHE.getDescripcion())
                    .totalRegistros(0L)
                    .fechaConsulta(LocalDateTime.now())
                    .origenDatos("SIN_DATOS")
                    .productos(Collections.emptyList())
                    .build();
        }

        // 4. Aplicar filtros opcionales con Streams y Predicates
        List<ProductoItemDTO> productos = resultado.getProductos() != null
                ? resultado.getProductos()
                : Collections.emptyList();

        productos = aplicarFiltros(productos, request);

        log.info("[POST] Catálogo entregado desde {}. Total tras filtros: {} productos",
                resultado.getOrigenDatos(), productos.size());

        CodigoRespuestaEnum codResp = (resultado.getCodigoRespuesta() != null)
                ? resultado.getCodigoRespuesta()
                : CodigoRespuestaEnum.EXITO;

        return ProductoListResponse.builder()
                .codigoRespuesta(codResp)
                .codigo(codResp.getCodigo())
                .mensaje(resultado.getMensaje())
                .totalRegistros((long) productos.size())
                .fechaConsulta(LocalDateTime.now())
                .origenDatos(resultado.getOrigenDatos())
                .productos(productos)
                .build();
    }

    /**
     * Intenta recuperar el catálogo desde Redis.
     *
     * @return {@link ProductoListResponse} si existe en caché, {@code null} si Redis está vacío o no disponible.
     */
    private ProductoListResponse leerDesdeRedis() {
        try {
            if (redisTemplate == null) return null;
            Object cached = redisTemplate.opsForValue().get(redisCatalogKey);
            if (cached instanceof ProductoListResponse cachedResp
                    && cachedResp.getProductos() != null
                    && !cachedResp.getProductos().isEmpty()) {
                log.info("[POST] Catálogo encontrado en CACHE REDIS. Total: {} productos",
                        cachedResp.getProductos().size());
                cachedResp.setCodigoRespuesta(CodigoRespuestaEnum.DATOS_DESDE_CACHE);
                cachedResp.setCodigo(CodigoRespuestaEnum.DATOS_DESDE_CACHE.getCodigo());
                cachedResp.setOrigenDatos("CACHE_REDIS");
                return cachedResp;
            }
            log.debug("[POST] Redis consultado pero no hay datos disponibles con llave '{}'", redisCatalogKey);
        } catch (Exception ex) {
            log.warn("[POST] Error al leer de Redis (se continúa con PostgreSQL): {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Intenta recuperar el catálogo desde PostgreSQL.
     *
     * @return {@link ProductoListResponse} si hay datos en BD, {@code null} si la tabla está vacía.
     */
    private ProductoListResponse leerDesdeBaseDeDatos() {
        try {
            if (productoCatalogoRepository == null) return null;
            List<ProductoCatalogoEntity> entidades = productoCatalogoRepository.findAll();
            if (!entidades.isEmpty()) {
                List<ProductoItemDTO> dtos = productoMapper.entityListToDtoList(entidades);
                log.info("[POST] Catálogo recuperado desde BASE DE DATOS PostgreSQL. Total: {} productos", dtos.size());
                return ProductoListResponse.builder()
                        .codigoRespuesta(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS)
                        .codigo(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS.getCodigo())
                        .mensaje(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS.getDescripcion())
                        .totalRegistros((long) dtos.size())
                        .fechaConsulta(LocalDateTime.now())
                        .origenDatos("BASE_DE_DATOS")
                        .productos(dtos)
                        .build();
            }
            log.debug("[POST] PostgreSQL consultado pero la tabla de catálogo está vacía.");
        } catch (Exception ex) {
            log.error("[POST] Error al leer de PostgreSQL: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Aplica los filtros opcionales del request sobre la lista de productos.
     *
     * @param productos lista fuente completa.
     * @param request   filtros a aplicar (pueden ser {@code null}).
     * @return lista filtrada.
     */
    private List<ProductoItemDTO> aplicarFiltros(List<ProductoItemDTO> productos, ProductoConsultaRequest request) {
        if (request == null || productos.isEmpty()) {
            return productos;
        }

        Predicate<ProductoItemDTO> filtro = p -> true; // Predicado inicial que acepta todo

        if (request.getTipoFront() != null) {
            filtro = filtro.and(esTipoFront(request.getTipoFront()));
            log.debug("[POST] Filtro aplicado: tipoFront={}", request.getTipoFront());
        }

        if (request.getServicio() != null && !request.getServicio().isBlank()) {
            String keyword = request.getServicio().trim().toLowerCase();
            filtro = filtro.and(p -> p.getServicio() != null && p.getServicio().toLowerCase().contains(keyword));
            log.debug("[POST] Filtro aplicado: servicio contiene '{}'", keyword);
        }

        if (request.getProducto() != null && !request.getProducto().isBlank()) {
            String keyword = request.getProducto().trim().toLowerCase();
            filtro = filtro.and(p -> p.getProducto() != null && p.getProducto().toLowerCase().contains(keyword));
            log.debug("[POST] Filtro aplicado: producto contiene '{}'", keyword);
        }

        final Predicate<ProductoItemDTO> filtroFinal = filtro;
        return productos.stream()
                .filter(Objects::nonNull)
                .filter(filtroFinal)
                .collect(Collectors.toList());
    }

    // ===================================================================
    // ESTRATEGIA DE CACHÉ Y PERSISTENCIA (REDIS & POSTGRESQL)
    // ===================================================================

    private void guardarEnRedis(ProductoListResponse response) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(redisCatalogKey, response, Duration.ofHours(redisTtlHours));
                log.info("Catálogo de {} productos almacenado exitosamente en caché Redis (TTL: {}h)",
                        (response.getProductos() != null ? response.getProductos().size() : 0), redisTtlHours);
            }
        } catch (Exception ex) {
            log.warn("No fue posible guardar en Redis (operando sin interrupción): {}", ex.getMessage());
        }
    }

    private void guardarEnBaseDeDatos(List<ProductoItemDTO> productos) {
        try {
            if (productoCatalogoRepository != null && productos != null && !productos.isEmpty()) {
                List<ProductoCatalogoEntity> entidades = productoMapper.dtoListToEntityList(productos);
                productoCatalogoRepository.saveAll(entidades);
                log.info("Catálogo de {} productos respaldado exitosamente en PostgreSQL (tabla productos_catalogo)", entidades.size());
            }
        } catch (Exception ex) {
            log.warn("No fue posible guardar respaldo en PostgreSQL: {}", ex.getMessage());
        }
    }

    // ===================================================================
    // ESTRATEGIA DE FALLBACK CON ENUM DE CÓDIGOS
    // ===================================================================

    private ProductoListResponse obtenerDesdeFallback(String motivo) {
        return obtenerDesdeFallback(motivo, null);
    }

    private ProductoListResponse obtenerDesdeFallback(String motivo, Throwable causaOriginal) {
        // 1. Intentar recuperar desde Redis
        try {
            if (redisTemplate != null) {
                Object cached = redisTemplate.opsForValue().get(redisCatalogKey);
                if (cached instanceof ProductoListResponse cachedResp && cachedResp.getProductos() != null && !cachedResp.getProductos().isEmpty()) {
                    log.info("Catálogo recuperado exitosamente desde CACHE REDIS (Modo Fallback)");
                    cachedResp.setCodigoRespuesta(CodigoRespuestaEnum.DATOS_DESDE_CACHE);
                    cachedResp.setCodigo(CodigoRespuestaEnum.DATOS_DESDE_CACHE.getCodigo());
                    cachedResp.setOrigenDatos("CACHE_REDIS");
                    return cachedResp;
                }
            }
        } catch (Exception redisEx) {
            log.warn("No fue posible leer de Redis durante fallback: {}", redisEx.getMessage());
        }

        // 2. Intentar recuperar desde PostgreSQL
        try {
            if (productoCatalogoRepository != null) {
                List<ProductoCatalogoEntity> entidades = productoCatalogoRepository.findAll();
                if (!entidades.isEmpty()) {
                    List<ProductoItemDTO> dtos = productoMapper.entityListToDtoList(entidades);
                    log.info("Catálogo recuperado exitosamente desde BASE DE DATOS PostgreSQL (Modo Fallback). Total: {}", dtos.size());
                    return ProductoListResponse.builder()
                            .codigoRespuesta(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS)
                            .codigo(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS.getCodigo())
                            .mensaje(CodigoRespuestaEnum.DATOS_DESDE_BASE_DE_DATOS.getDescripcion())
                            .totalRegistros((long) dtos.size())
                            .fechaConsulta(LocalDateTime.now())
                            .origenDatos("BASE_DE_DATOS")
                            .productos(dtos)
                            .build();
                }
            }
        } catch (Exception dbEx) {
            log.error("Fallo al leer base de datos PostgreSQL durante fallback: {}", dbEx.getMessage());
        }

        // 3. Si no hay datos, propagar la excepción
        if (causaOriginal instanceof ExternalServiceTimeoutException timeoutEx) {
            throw timeoutEx;
        }
        if (causaOriginal != null) {
            throw new ExternalServiceException("Servicio externo no disponible y no se encontraron datos en caché/BD: " + motivo, causaOriginal, 502);
        }
        throw new ExternalServiceException("Servicio externo no disponible y no se encontraron datos en caché/BD: " + motivo, 502);
    }

    @Override
    public ProductoListResponse consultarProductosParaFrontend() {
        log.info("Consultando productos ordenados ascendentemente para el frontend");
        ProductoListResponse response = consultarListaProductos();
        if (response != null && response.getProductos() != null) {
            List<ProductoItemDTO> ordenados = new ArrayList<>(response.getProductos());
            ordenados.sort(java.util.Comparator.comparing(
                    p -> p.getProducto() != null ? p.getProducto() : "",
                    String.CASE_INSENSITIVE_ORDER
            ));
            response.setProductos(ordenados);
            response.setMensaje("Catálogo para frontend ordenado ascendentemente");
        }
        return response;
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
