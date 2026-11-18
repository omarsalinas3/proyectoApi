package com.proyecto.servicios.repositorys.cliente;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Busca el registro de autenticación por el hash de búsqueda del nombre de usuario.
     */
    Optional<LoginEntity> findByUsernameHash(String usernameHash);

    /**
     * Busca las credenciales asociadas a un identificador de usuario.
     */
    Optional<LoginEntity> findByUsuarioId(Long usuarioId);

    /**
     * Valida la existencia de un nombre de usuario.
     */
    boolean existsByUsernameHash(String usernameHash);

    /**
     * Registros con algún dato todavía en el formato de cifrado anterior.
     */
    @Query("select l from LoginEntity l where l.usernameHash is null or l.usernameCifrado not like 'v2:%'"
            + " or (l.totpSecretoCifrado is not null and l.totpSecretoCifrado not like 'v2:%')"
            + " or (l.datosBiometricosCifrados is not null and l.datosBiometricosCifrados not like 'v2:%')")
    List<LoginEntity> findPendientesDeMigrarCifrado();

    /**
     * Busca las credenciales de un cliente cargando también el cliente (para validar su estatus activo).
     */
    @Query("select l from LoginEntity l join fetch l.usuario u where u.id = :usuarioId")
    Optional<LoginEntity> findConUsuarioByUsuarioId(@Param("usuarioId") Long usuarioId);
}
