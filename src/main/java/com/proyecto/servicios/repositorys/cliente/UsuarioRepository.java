package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Clientes Personas Físicas (tabla {@code usuarios}).
 * Las búsquedas por criterios combinables se resuelven con {@link UsuarioSpecifications}.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long>, JpaSpecificationExecutor<UsuarioEntity> {

    /**
     * Busca un cliente por su clave CURP.
     */
    Optional<UsuarioEntity> findByCurp(String curp);

    /**
     * Busca un cliente por su clave RFC.
     */
    Optional<UsuarioEntity> findByRfc(String rfc);

    /**
     * Busca un cliente por su correo electrónico.
     */
    Optional<UsuarioEntity> findByCorreo(String correo);

    /**
     * Valida la existencia previa de un CURP.
     */
    boolean existsByCurp(String curp);

    /**
     * Valida la existencia previa de un RFC.
     */
    boolean existsByRfc(String rfc);

    /**
     * Valida la existencia previa de un correo electrónico en otros registros.
     */
    boolean existsByCorreoAndIdNot(String correo, Long id);

    /**
     * Valida la existencia previa de un correo electrónico.
     */
    boolean existsByCorreo(String correo);
}
