package com.proyecto.servicios.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Resultado paginado de una consulta.
 *
 * @param <T> tipo de los elementos.
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Resultado paginado de una consulta")
public class PaginaResponse<T> {

    @Schema(description = "Elementos de la página solicitada")
    private List<T> contenido;

    @Schema(description = "Número de página (inicia en 0)", example = "0")
    private int pagina;

    @Schema(description = "Tamaño de página solicitado", example = "20")
    private int tamanio;

    @Schema(description = "Total de elementos que cumplen los filtros", example = "1")
    private long totalElementos;

    @Schema(description = "Total de páginas", example = "1")
    private int totalPaginas;

    /**
     * Construye la respuesta a partir de una página de entidades, convirtiendo cada elemento a DTO.
     */
    public static <E, T> PaginaResponse<T> de(Page<E> pagina, Function<E, T> mapper) {
        return PaginaResponse.<T>builder()
                .contenido(pagina.getContent().stream().map(mapper).toList())
                .pagina(pagina.getNumber())
                .tamanio(pagina.getSize())
                .totalElementos(pagina.getTotalElements())
                .totalPaginas(pagina.getTotalPages())
                .build();
    }
}
