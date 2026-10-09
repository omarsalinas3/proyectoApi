package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.model.cliente.dto.CuentaDTO;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.model.cliente.dto.SaldoDTO;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.model.cliente.response.CuentaResponse;
import com.proyecto.servicios.model.cliente.response.SaldoResponse;
import org.mapstruct.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mapper MapStruct para transformaciones entre DTOs de Clientes, Cuentas, Domicilios y Entidades JPA.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClienteMapper {

    /**
     * Mapea la petición de registro hacia la entidad {@link UsuarioEntity}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "activo", constant = "true")
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "login", ignore = true)
    UsuarioEntity requestToEntity(ClienteRegistroRequest request);

    /**
     * Mapea un DTO de domicilio hacia su entidad JPA.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    DomicilioEntity domicilioDtoToEntity(DomicilioDTO dto);

    /**
     * Mapea una entidad de domicilio hacia su DTO de salida.
     */
    DomicilioDTO domicilioEntityToDto(DomicilioEntity entity);

    /**
     * Mapea una entidad de cuenta bancaria hacia su DTO de salida.
     */
    CuentaDTO cuentaEntityToDto(CuentaEntity entity);

    /**
     * Mapea una entidad de saldo hacia su DTO de salida.
     */
    @Mapping(target = "numeroCuenta", source = "cuenta.numeroCuenta")
    SaldoDTO saldoEntityToDto(SaldoEntity entity);

    /**
     * Mapea una entidad {@link UsuarioEntity} hacia el response {@link ClienteResponse}.
     */
    @Mapping(target = "codigoRespuesta", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "mensaje", ignore = true)
    @Mapping(target = "nombreCompleto", expression = "java(construirNombreCompleto(entity))")
    @Mapping(target = "domicilio", source = "domicilio")
    @Mapping(target = "cuenta", source = "cuenta")
    @Mapping(target = "saldo", source = "cuenta.saldo")
    ClienteResponse entityToResponse(UsuarioEntity entity);

    /**
     * Mapea una lista de entidades de usuario a lista de respuestas.
     */
    List<ClienteResponse> entityListToResponseList(List<UsuarioEntity> entities);

    /**
     * Mapea una entidad de cuenta a CuentaResponse.
     */
    @Mapping(target = "codigoRespuesta", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "mensaje", ignore = true)
    @Mapping(target = "titular", expression = "java(construirNombreCompleto(entity.getUsuario()))")
    @Mapping(target = "curpTitular", source = "usuario.curp")
    @Mapping(target = "cuenta", source = "entity")
    CuentaResponse toCuentaResponse(CuentaEntity entity);

    /**
     * Mapea una entidad de saldo a SaldoResponse.
     */
    @Mapping(target = "codigoRespuesta", ignore = true)
    @Mapping(target = "codigo", ignore = true)
    @Mapping(target = "mensaje", ignore = true)
    @Mapping(target = "saldo", source = "entity")
    SaldoResponse toSaldoResponse(SaldoEntity entity);

    /**
     * Actualiza una entidad de domicilio existente desde un DTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usuario", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    void updateDomicilioFromDto(DomicilioDTO dto, @MappingTarget DomicilioEntity entity);

    /**
     * Actualiza los datos permitidos de un {@link UsuarioEntity} desde un {@link ClienteUpdateRequest}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "curp", ignore = true)
    @Mapping(target = "rfc", ignore = true)
    @Mapping(target = "fechaNacimiento", ignore = true)
    @Mapping(target = "ineDocumentoUrl", ignore = true)
    @Mapping(target = "activo", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    @Mapping(target = "fechaModificacion", ignore = true)
    @Mapping(target = "domicilio", ignore = true)
    @Mapping(target = "cuenta", ignore = true)
    @Mapping(target = "login", ignore = true)
    void updateEntityFromRequest(ClienteUpdateRequest request, @MappingTarget UsuarioEntity entity);

    /**
     * Método utilitario para concatenar nombres y apellidos.
     */
    default String construirNombreCompleto(UsuarioEntity entity) {
        if (entity == null) return null;
        StringBuilder sb = new StringBuilder();
        if (entity.getNombre() != null) {
            sb.append(entity.getNombre().trim());
        }
        if (entity.getSegundoNombre() != null && !entity.getSegundoNombre().isBlank()) {
            sb.append(" ").append(entity.getSegundoNombre().trim());
        }
        if (entity.getApellidoPaterno() != null && !entity.getApellidoPaterno().isBlank()) {
            sb.append(" ").append(entity.getApellidoPaterno().trim());
        }
        if (entity.getApellidoMaterno() != null && !entity.getApellidoMaterno().isBlank()) {
            sb.append(" ").append(entity.getApellidoMaterno().trim());
        }
        return sb.toString();
    }

    /**
     * Mapea un OffsetDateTime a LocalDate para devolver únicamente la fecha sin timestamp.
     */
    default LocalDate map(OffsetDateTime value) {
        return value != null ? value.toLocalDate() : null;
    }
}
