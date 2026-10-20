package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Consultas dinámicas de cuentas: cada filtro informado en {@link CuentaBusquedaRequest} agrega una
 * condición y todas se combinan con AND.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public final class CuentaSpecifications {

    private CuentaSpecifications() {
    }

    public static Specification<CuentaEntity> conFiltros(CuentaBusquedaRequest filtros) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (filtros.getNumeroCuenta() != null && !filtros.getNumeroCuenta().isBlank()) {
                condiciones.add(cb.equal(root.get("numeroCuenta"), filtros.getNumeroCuenta().trim()));
            }
            if (filtros.getClienteId() != null) {
                condiciones.add(cb.equal(root.get("usuario").get("id"), filtros.getClienteId()));
            }
            if (filtros.getEstatus() != null && !filtros.getEstatus().isBlank()) {
                condiciones.add(cb.equal(root.get("estatus"), filtros.getEstatus()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
