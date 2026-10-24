package com.proyecto.servicios.repositorys.transaccion;

import com.proyecto.servicios.entity.transaccion.TransaccionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA de las transacciones enviadas a GestoPago (tabla {@code transacciones}).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Repository
public interface TransaccionRepository extends JpaRepository<TransaccionEntity, Long> {

    Optional<TransaccionEntity> findByUpc(String upc);

    boolean existsByUpc(String upc);
}
