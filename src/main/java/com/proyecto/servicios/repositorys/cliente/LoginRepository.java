package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la autenticación y control de sesiones (tabla {@code login}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface LoginRepository extends JpaRepository<LoginEntity, Long> {

    /**
     * Busca el registro de autenticación por el nombre de usuario cifrado.
     */
    Optional<LoginEntity> findByUsernameCifrado(String usernameCifrado);

    /**
     * Busca las credenciales asociadas a un identificador de usuario.
     */
    Optional<LoginEntity> findByUsuarioId(Long usuarioId);

    /**
     * Valida la existencia de un nombre de usuario.
     */
    boolean existsByUsernameCifrado(String usernameCifrado);

    /**
     * Busca las credenciales de un cliente cargando también el cliente (para validar su estatus activo).
     */
    @Query("select l from LoginEntity l join fetch l.usuario u where u.id = :usuarioId")
    Optional<LoginEntity> findConUsuarioByUsuarioId(@Param("usuarioId") Long usuarioId);
}
