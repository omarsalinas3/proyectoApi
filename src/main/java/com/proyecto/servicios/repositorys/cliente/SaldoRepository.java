package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.SaldoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la gestión de Saldos (tabla {@code saldos}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface SaldoRepository extends JpaRepository<SaldoEntity, Long> {

    /**
     * Busca los saldos asociados a una cuenta bancaria por su ID.
     */
    Optional<SaldoEntity> findByCuentaId(Long cuentaId);

    /**
     * Busca los saldos asociados a un usuario.
     */
    Optional<SaldoEntity> findByUsuarioId(Long usuarioId);
}
