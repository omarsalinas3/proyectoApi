package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Clientes Personas Físicas (tabla {@code usuarios}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {

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
     * Busca un cliente a través de su número de cuenta bancaria.
     */
    @Query("SELECT u FROM UsuarioEntity u JOIN u.cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<UsuarioEntity> findByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    /**
     * Consulta todos los clientes con estatus activo (baja lógica).
     */
    List<UsuarioEntity> findByActivoTrue();

    /**
     * Consulta clientes registrados en un rango de fechas específico.
     */
    List<UsuarioEntity> findByFechaRegistroBetween(OffsetDateTime fechaInicio, OffsetDateTime fechaFin);

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
