package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Cuentas Bancarias (tabla {@code cuenta}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long> {

    /**
     * Busca una cuenta bancaria por su número de cuenta de 16 dígitos.
     */
    Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);

    /**
     * Consulta todas las cuentas con estatus {@code ACTIVA}.
     */
    List<CuentaEntity> findByEstatus(String estatus);

    /**
     * Consulta el saldo disponible de una cuenta por su número de cuenta.
     */
    @Query("SELECT c.saldoDisponible FROM CuentaEntity c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<BigDecimal> findSaldoDisponibleByNumeroCuenta(@Param("numeroCuenta") String numeroCuenta);

    /**
     * Valida si un número de cuenta ya existe en la base de datos.
     */
    boolean existsByNumeroCuenta(String numeroCuenta);
}
