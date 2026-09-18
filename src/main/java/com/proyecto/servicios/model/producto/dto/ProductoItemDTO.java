package com.proyecto.servicios.model.producto.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) que representa un producto o servicio obtenido del endpoint
 * {@code GET /sistema/service/getProductList.do} de PuntoRed / GestoPago.
 *
 * <p><strong>Estructura según especificación técnica Postman:</strong>
 * <ul>
 *   <li>{@code idProducto}: Identificador numérico del producto ({@link Long}).</li>
 *   <li>{@code idServicio}: Identificador numérico del servicio ({@link Integer}).</li>
 *   <li>{@code idCatTipoServicio}: Tipo de servicio / categoría ({@link Integer}).</li>
 *   <li>{@code tipoFront}: Tipo de frontend / payload requerido (1=Teléfono, 2=Referencia, etc.) ({@link Integer}).</li>
 *   <li>{@code producto}: Nombre del producto ({@link String}).</li>
 *   <li>{@code servicio}: Nombre del servicio o compañía ({@link String}).</li>
 *   <li>{@code precio}: Precio o comisión del producto ({@link BigDecimal}).</li>
 *   <li>{@code tipoReferencia}: Identificador de origen de la referencia (a, b, c, ab, bc) ({@link String}).</li>
 *   <li>{@code hasDigitoVerificador}: Indicador de dígito verificador ({@link Boolean}).</li>
 *   <li>{@code showAyuda}: Indicador de ayuda en interfaz ({@link Boolean}).</li>
 *   <li>{@code legend}: Instrucciones o leyenda para canje/pago ({@link String}).</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Identificador numérico único del producto.
     */
    @JsonProperty("idProducto")
    private Long idProducto;

    /**
     * Identificador numérico del servicio al que pertenece el producto.
     */
    @JsonProperty("idServicio")
    private Integer idServicio;

    /**
     * Identificador numérico de la categoría o tipo de servicio (ej. 1=Movistar, 2=Telcel, 15=Agua, etc.).
     */
    @JsonProperty("idCatTipoServicio")
    private Integer idCatTipoServicio;

    /**
     * Identificador de comportamiento del payload (1=Recarga/Teléfono obligatorio, 2=Pago/Referencia obligatoria).
     */
    @JsonProperty("tipoFront")
    private Integer tipoFront;

    /**
     * Nombre descriptivo del producto.
     */
    @JsonProperty("producto")
    private String producto;

    /**
     * Nombre descriptivo del servicio o compañía proveedora (ej. "Amazon", "AGUAKAN (Cancun)", "Telcel").
     */
    @JsonProperty("servicio")
    private String servicio;

    /**
     * Precio final del producto o comisión generada por la transacción con precisión exacta.
     */
    @JsonProperty("precio")
    private BigDecimal precio;

    /**
     * Clasificación del tipo de referencia requerida (a, b, c, ab, bc).
     */
    @JsonProperty("tipoReferencia")
    private String tipoReferencia;

    /**
     * Indica si el producto requiere validación de dígito verificador.
     */
    @JsonProperty("hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    /**
     * Indica si se debe mostrar mensaje de ayuda en pantalla.
     */
    @JsonProperty("showAyuda")
    private Boolean showAyuda;

    /**
     * Leyenda informativa o instrucciones para el canje / pago del producto.
     */
    @JsonProperty("legend")
    private String legend;
}
