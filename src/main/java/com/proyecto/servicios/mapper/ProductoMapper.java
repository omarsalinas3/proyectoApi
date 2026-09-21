package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import com.proyecto.servicios.model.producto.dto.ProductoItemDTO;
import com.proyecto.servicios.model.producto.xml.ProductoXml;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapper MapStruct para transformaciones entre modelos XML (JAXB), DTOs de negocio y Entidades JPA.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        imports = {LocalDateTime.class}
)
public interface ProductoMapper {

    /**
     * Mapea un elemento {@link ProductoXml} parseado con JAXB a {@link ProductoItemDTO}.
     *
     * @param xml modelo JAXB.
     * @return DTO tipado.
     */
    ProductoItemDTO xmlToDto(ProductoXml xml);

    /**
     * Mapea una lista de elementos XML a lista de DTOs.
     *
     * @param xmlList lista de modelos JAXB.
     * @return lista de DTOs de producto.
     */
    List<ProductoItemDTO> xmlListToDtoList(List<ProductoXml> xmlList);

    /**
     * Mapea un {@link ProductoItemDTO} hacia la entidad JPA {@link ProductoCatalogoEntity}.
     *
     * @param dto DTO de producto.
     * @return entidad JPA lista para persistir.
     */
    @Mapping(target = "fechaActualizacion", expression = "java(LocalDateTime.now())")
    ProductoCatalogoEntity dtoToEntity(ProductoItemDTO dto);

    /**
     * Mapea una lista de DTOs a entidades JPA para inserción en lote.
     *
     * @param dtoList lista de DTOs.
     * @return lista de entidades JPA.
     */
    List<ProductoCatalogoEntity> dtoListToEntityList(List<ProductoItemDTO> dtoList);

    /**
     * Mapea una entidad JPA {@link ProductoCatalogoEntity} recuperada de base de datos a {@link ProductoItemDTO}.
     *
     * @param entity entidad JPA.
     * @return DTO de producto.
     */
    ProductoItemDTO entityToDto(ProductoCatalogoEntity entity);

    /**
     * Mapea una lista de entidades JPA a DTOs.
     *
     * @param entityList lista de entidades.
     * @return lista de DTOs.
     */
    List<ProductoItemDTO> entityListToDtoList(List<ProductoCatalogoEntity> entityList);
}
