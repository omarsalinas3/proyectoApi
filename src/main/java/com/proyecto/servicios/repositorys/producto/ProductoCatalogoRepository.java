package com.proyecto.servicios.repositorys.producto;

import com.proyecto.servicios.entity.producto.ProductoCatalogoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para la entidad {@link ProductoCatalogoEntity}.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface ProductoCatalogoRepository extends JpaRepository<ProductoCatalogoEntity, Long> {

    /**
     * Consulta productos filtrados por su identificador de tipo front.
     *
     * @param tipoFront identificador de presentación en frontend (1, 2, 3...).
     * @return lista de entidades coincidentes.
     */
    List<ProductoCatalogoEntity> findByTipoFront(Integer tipoFront);
}
