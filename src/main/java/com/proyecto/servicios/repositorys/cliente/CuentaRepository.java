package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Cuentas Bancarias (tabla {@code cuenta}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface CuentaRepository extends JpaRepository<CuentaEntity, Long>, JpaSpecificationExecutor<CuentaEntity> {

    /**
     * Busca una cuenta bancaria por su número de cuenta de 16 dígitos.
     */
    Optional<CuentaEntity> findByNumeroCuenta(String numeroCuenta);



    /**
     * Valida si un número de cuenta ya existe en la base de datos.
     */
    boolean existsByNumeroCuenta(String numeroCuenta);
}
