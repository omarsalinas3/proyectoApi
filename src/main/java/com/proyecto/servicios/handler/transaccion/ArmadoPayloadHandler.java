package com.proyecto.servicios.handler.transaccion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.exception.ProductoNoDisponibleException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.gestopago.SendTxPayload;
import com.proyecto.servicios.model.transaccion.request.TransaccionRequest;
import com.proyecto.servicios.repositorys.producto.ProductoCatalogoRepository;
import com.proyecto.servicios.repositorys.transaccion.TransaccionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * Nodo «Sen Tx»: arma el payload de {@code sendTx} con las reglas de la documentación de GestoPago.
 *
 * <ul>
 *   <li>El producto debe existir en el catálogo con el mismo idServicio; si tiene precio fijo, el monto debe
 *   coincidir.</li>
 *   <li>Recargas ({@code tipoFront} 1): se envía el teléfono capturado, sin referencia ni montoPago.</li>
 *   <li>Pagos de servicios como luz o agua ({@code tipoFront} 2): el teléfono va fijo en "1111111111" y se envían
 *   la {@code referencia} del recibo y el {@code montoPago}.</li>
 *   <li>{@code idCatTipoServicio} 10 u 11: el teléfono va fijo en "#1111111111".</li>
 *   <li>{@code horaLocal}: yyyyMMddHHmmss generada en el servidor; {@code upc}: único por operación (10
 *   caracteres); {@code unidad}: sucursal configurada (3 a 25).</li>
 * </ul>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Component
public class ArmadoPayloadHandler implements PasoTransaccionHandler {

    static final int TIPO_FRONT_PAGO_SERVICIO = 2;
    static final Set<Integer> TIPOS_SERVICIO_TELEFONO_ALMOHADILLA = Set.of(10, 11);
    static final String TELEFONO_PAGO_SERVICIO = "1111111111";
    static final String TELEFONO_TIPO_SERVICIO_ESPECIAL = "#1111111111";

    private static final DateTimeFormatter FORMATO_HORA_LOCAL = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String PREFIJO_UPC = "TX";
    static final int LONGITUD_UPC = 10;
    private static final String ALFABETO_UPC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final ProductoCatalogoRepository productoRepository;
    private final TransaccionRepository transaccionRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String unidad;
    private final ZoneId zonaHoraria;
    private final SecureRandom random = new SecureRandom();

    public ArmadoPayloadHandler(ProductoCatalogoRepository productoRepository,
                                TransaccionRepository transaccionRepository,
                                ObjectMapper objectMapper,
                                Clock clock,
                                @Value("${gestopago.transaccion.unidad}") String unidad,
                                @Value("${gestopago.transaccion.zona-horaria:America/Mexico_City}") String zonaHoraria) {
        if (unidad == null || unidad.length() < SendTxPayload.UNIDAD_MIN || unidad.length() > SendTxPayload.UNIDAD_MAX) {
            throw new IllegalStateException("gestopago.transaccion.unidad debe tener entre 3 y 25 caracteres");
        }
        this.productoRepository = productoRepository;
        this.transaccionRepository = transaccionRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.unidad = unidad;
        this.zonaHoraria = ZoneId.of(zonaHoraria);
    }

    @Override
    public void manejar(ContextoTransaccion contexto) {
        TransaccionRequest request = contexto.getRequest();
        ProductoCatalogoEntity producto = productoRepository.findById(request.getIdProducto())
                .filter(p -> request.getIdServicio().equals(p.getIdServicio()))
                .orElseThrow(() -> new ProductoNoDisponibleException(String.format(
                        "El producto %d del servicio %d no existe en el catálogo; consulte GET /api/v1/productos",
                        request.getIdProducto(), request.getIdServicio())));

        BigDecimal monto = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        boolean pagoDeServicio = Integer.valueOf(TIPO_FRONT_PAGO_SERVICIO).equals(producto.getTipoFront());
        // En recargas el precio del catálogo es el importe de la recarga; en pagos de servicio el monto es el del
        // recibo (el "precio" del catálogo no aplica), por eso solo se exige coincidencia en recargas.
        BigDecimal precio = producto.getPrecio();
        if (!pagoDeServicio && precio != null && precio.signum() > 0 && precio.compareTo(monto) != 0) {
            throw new ProductoNoDisponibleException(String.format(
                    "El producto \"%s\" tiene precio fijo de $%s; el monto debe ser exactamente ese valor",
                    producto.getProducto(), precio.setScale(2, RoundingMode.HALF_UP)));
        }

        if (pagoDeServicio && sinTexto(request.getReferencia())) {
            throw new ValidacionNegocioException(String.format(
                    "\"%s\" es un pago de servicio: indique la referencia impresa en el recibo", nombre(producto)));
        }
        if (!pagoDeServicio && !usaTelefonoFijo(producto) && sinTexto(request.getTelefono())) {
            throw new ValidacionNegocioException(String.format(
                    "\"%s\" es una recarga: indique el número telefónico a recargar", nombre(producto)));
        }

        SendTxPayload payload = SendTxPayload.builder()
                .idProducto(producto.getIdProducto())
                .idServicio(producto.getIdServicio())
                .telefono(telefonoPara(producto, request.getTelefono()))
                .horaLocal(ZonedDateTime.now(clock.withZone(zonaHoraria)).format(FORMATO_HORA_LOCAL))
                .referencia(pagoDeServicio ? request.getReferencia() : null)
                .montoPago(pagoDeServicio ? monto : null)
                .upc(generarUpc())
                .unidad(unidad)
                .build();
        validarLongitudes(payload);

        contexto.setProducto(producto);
        contexto.setPayload(payload);
        contexto.setPayloadJson(aJson(payload));
    }

    static String telefonoPara(ProductoCatalogoEntity producto, String telefonoCapturado) {
        if (usaTelefonoFijo(producto)) {
            return TELEFONO_TIPO_SERVICIO_ESPECIAL;
        }
        if (Integer.valueOf(TIPO_FRONT_PAGO_SERVICIO).equals(producto.getTipoFront())) {
            return TELEFONO_PAGO_SERVICIO;
        }
        return telefonoCapturado;
    }

    private static boolean usaTelefonoFijo(ProductoCatalogoEntity producto) {
        return producto.getIdCatTipoServicio() != null && TIPOS_SERVICIO_TELEFONO_ALMOHADILLA.contains(producto.getIdCatTipoServicio());
    }

    private static boolean sinTexto(String valor) {
        return valor == null || valor.isBlank();
    }

    private static String nombre(ProductoCatalogoEntity producto) {
        return producto.getProducto() != null ? producto.getProducto() : "Producto " + producto.getIdProducto();
    }

    private String generarUpc() {
        String upc;
        do {
            StringBuilder sb = new StringBuilder(PREFIJO_UPC);
            while (sb.length() < LONGITUD_UPC) {
                sb.append(ALFABETO_UPC.charAt(random.nextInt(ALFABETO_UPC.length())));
            }
            upc = sb.toString();
        } while (transaccionRepository.existsByUpc(upc));
        return upc;
    }

    private static void validarLongitudes(SendTxPayload p) {
        verificarLongitud("telefono", p.getTelefono(), SendTxPayload.TELEFONO_MIN, SendTxPayload.TELEFONO_MAX);
        verificarLongitud("horaLocal", p.getHoraLocal(), 1, SendTxPayload.HORA_LOCAL_MAX);
        verificarLongitud("upc", p.getUpc(), SendTxPayload.UPC_MIN, SendTxPayload.UPC_MAX);
        verificarLongitud("unidad", p.getUnidad(), SendTxPayload.UNIDAD_MIN, SendTxPayload.UNIDAD_MAX);
        if (p.getReferencia() != null) {
            verificarLongitud("referencia", p.getReferencia(), 1, SendTxPayload.REFERENCIA_MAX);
        }
    }

    private static void verificarLongitud(String campo, String valor, int minimo, int maximo) {
        if (valor == null || valor.length() < minimo || valor.length() > maximo) {
            throw new ProductoNoDisponibleException(String.format(
                    "El campo %s debe tener entre %d y %d caracteres para GestoPago", campo, minimo, maximo));
        }
    }

    private String aJson(SendTxPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible serializar el payload de sendTx", e);
        }
    }
}
