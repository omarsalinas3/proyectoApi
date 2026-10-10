package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Domicilios (tabla {@code domicilios}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface DomicilioRepository extends JpaRepository<DomicilioEntity, Long> {

    /**
     * Busca el domicilio de un usuario por su ID.
     */
    Optional<DomicilioEntity> findByUsuarioId(Long usuarioId);
}
