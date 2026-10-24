package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * Obtiene la cuenta bloqueando su fila hasta el fin de la transacción de base de datos, para que dos
     * operaciones simultáneas no puedan disponer del mismo saldo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaEntity c where c.numeroCuenta = :numeroCuenta")
    Optional<CuentaEntity> findByNumeroCuentaParaActualizar(@Param("numeroCuenta") String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CuentaEntity c where c.id = :id")
    Optional<CuentaEntity> findByIdParaActualizar(@Param("id") Long id);
}
