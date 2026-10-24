package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Consultas dinámicas de clientes: cada filtro informado en {@link ClienteBusquedaRequest} agrega una
 * condición y todas se combinan con AND. Los valores se envían como parámetros (sin concatenar SQL).
 *
 * <p>Las fechas del filtro son días calendario en la zona horaria del negocio (ej. America/Mexico_City): un
 * cliente registrado a las 18:40 hora de México aparece en "hoy" aunque en UTC ya sea el día siguiente.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public final class UsuarioSpecifications {

    private UsuarioSpecifications() {
    }

    public static Specification<UsuarioEntity> conFiltros(ClienteBusquedaRequest filtros, ZoneId zona) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (filtros.getId() != null) {
                condiciones.add(cb.equal(root.get("id"), filtros.getId()));
            }
            if (tieneTexto(filtros.getCurp())) {
                condiciones.add(cb.equal(root.get("curp"), filtros.getCurp().trim().toUpperCase(Locale.ROOT)));
            }
            if (tieneTexto(filtros.getRfc())) {
                condiciones.add(cb.equal(root.get("rfc"), filtros.getRfc().trim().toUpperCase(Locale.ROOT)));
            }
            if (tieneTexto(filtros.getCorreo())) {
                condiciones.add(cb.equal(root.get("correo"), filtros.getCorreo().trim().toLowerCase(Locale.ROOT)));
            }
            if (tieneTexto(filtros.getNumeroCuenta())) {
                Join<UsuarioEntity, CuentaEntity> cuenta = root.join("cuenta", JoinType.INNER);
                condiciones.add(cb.equal(cuenta.get("numeroCuenta"), filtros.getNumeroCuenta().trim()));
            }
            if (tieneTexto(filtros.getNombre())) {
                String patron = "%" + filtros.getNombre().trim().toLowerCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("nombre")), patron),
                        cb.like(cb.lower(cb.coalesce(root.get("segundoNombre"), "")), patron),
                        cb.like(cb.lower(root.get("apellidoPaterno")), patron),
                        cb.like(cb.lower(root.get("apellidoMaterno")), patron)));
            }
            if (filtros.getActivo() != null) {
                condiciones.add(cb.equal(root.get("activo"), filtros.getActivo()));
            }
            if (filtros.getFechaRegistroDesde() != null) {
                OffsetDateTime desde = filtros.getFechaRegistroDesde().atStartOfDay(zona).toOffsetDateTime();
                condiciones.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde));
            }
            if (filtros.getFechaRegistroHasta() != null) {
                OffsetDateTime hastaExclusivo = filtros.getFechaRegistroHasta().plusDays(1).atStartOfDay(zona).toOffsetDateTime();
                condiciones.add(cb.lessThan(root.get("fechaRegistro"), hastaExclusivo));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
