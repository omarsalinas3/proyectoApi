package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Implementación del servicio de autenticación y gestión de sesiones dinámicas.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final LoginRepository loginRepository;
    private final UsuarioRepository usuarioRepository;
    private final CryptoService cryptoService;

    @Value("${seguridad.sesion.default-timeout-seconds:300}")
    private Integer defaultTimeoutSeconds;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ValidacionNegocioException("El nombre de usuario es obligatorio");
        }

        String rawUsername = request.getUsername().trim();
        log.info("Intento de inicio de sesión para el usuario: {}, Bandera: {}", rawUsername, request.getBandera());

        // Buscar LoginEntity por username cifrado o por usuario asociado (curp / correo)
        String usernameCifrado = cryptoService.encrypt(rawUsername);
        Optional<LoginEntity> optLogin = loginRepository.findByUsernameCifrado(usernameCifrado);

        if (optLogin.isEmpty()) {
            // Intentar buscar por correo o CURP en usuarios
            Optional<UsuarioEntity> optUser = usuarioRepository.findByCorreo(rawUsername);
            if (optUser.isEmpty()) {
                optUser = usuarioRepository.findByCurp(rawUsername);
            }
            if (optUser.isPresent() && optUser.get().getLogin() != null) {
                optLogin = Optional.of(optUser.get().getLogin());
            }
        }

        LoginEntity loginEntity = optLogin
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o credenciales incorrectas"));

        int bandera = request.getBandera() != null ? request.getBandera() : 1;

        if (bandera == 1) {
            // Bandera 1: Autenticación por contraseña
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new ValidacionNegocioException("La contraseña es obligatoria para la bandera 1 (Contraseña)");
            }
            if (!cryptoService.verifyPassword(request.getPassword(), loginEntity.getPasswordHash())) {
                log.warn("Contraseña incorrecta para el usuario: {}", rawUsername);
                throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
            }
        } else if (bandera == 2) {
            // Bandera 2: Autenticación por Face ID biométrico
            if (request.getFaceIdBiometrico() == null) {
                throw new ValidacionNegocioException("El identificador faceIdBiometrico es obligatorio para la bandera 2 (Biométrico Face ID)");
            }

            BigDecimal registeredPrecision = loginEntity.getDatosBiometricosPrecision();
            if (registeredPrecision != null) {
                BigDecimal reqBio = BigDecimal.valueOf(request.getFaceIdBiometrico());
                if (registeredPrecision.compareTo(reqBio) != 0) {
                    log.warn("Fallo de coincidencia Face ID biométrico para el usuario: {}", rawUsername);
                    throw new CredencialesInvalidasException("Datos biométricos Face ID no coinciden con los registrados");
                }
            }
            log.info("Autenticación biométrica Face ID exitosa para usuario: {}", rawUsername);
        } else {
            throw new ValidacionNegocioException("Bandera de autenticación no soportada: " + bandera + ". Use 1 = Contraseña, 2 = Biométrico Face ID");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        int timeout = loginEntity.getTiempoInactividadSegundos() != null ? loginEntity.getTiempoInactividadSegundos() : defaultTimeoutSeconds;

        loginEntity.setSesionActiva(true);
        loginEntity.setUltimaActividad(ahora);
        loginRepository.save(loginEntity);

        log.info("Inicio de sesión exitoso para usuario: {}. Sesión activa = true", rawUsername);

        return LoginResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Inicio de sesión exitoso (Bandera " + bandera + ")")
                .usuarioId(loginEntity.getUsuario() != null ? loginEntity.getUsuario().getId() : null)
                .username(rawUsername)
                .sesionActiva(true)
                .tiempoInactividadSegundos(timeout)
                .segundosRestantesInactividad((long) timeout)
                .ultimaActividad(ahora)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verificarEstadoSesion(String username, Integer tiempoInactividadSegundos) {
        log.debug("Verificando estado de sesión para usuario: {}", username);
        String usernameCifrado = cryptoService.encrypt(username.trim());

        LoginEntity loginEntity = loginRepository.findByUsernameCifrado(usernameCifrado)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no registrado en el sistema"));

        if (Boolean.FALSE.equals(loginEntity.getSesionActiva())) {
            log.warn("Intento de acceso con sesión inactiva para el usuario: {}", username);
            throw new SesionExpiradaException("La sesión no se encuentra activa");
        }

        int limiteSegundos = (tiempoInactividadSegundos != null && tiempoInactividadSegundos > 0)
                ? tiempoInactividadSegundos
                : (loginEntity.getTiempoInactividadSegundos() != null ? loginEntity.getTiempoInactividadSegundos() : defaultTimeoutSeconds);

        OffsetDateTime ultimaActividad = loginEntity.getUltimaActividad() != null 
                ? loginEntity.getUltimaActividad() 
                : loginEntity.getFechaCreacion();

        long segundosInactivo = Duration.between(ultimaActividad, OffsetDateTime.now()).getSeconds();

        if (segundosInactivo > limiteSegundos) {
            log.warn("Sesión expirada para usuario: {}. Inactividad: {}s (Límite: {}s)", username, segundosInactivo, limiteSegundos);
            loginEntity.setSesionActiva(false);
            loginRepository.save(loginEntity);
            throw new SesionExpiradaException(
                    String.format("La sesión ha expirado por inactividad (%d segundos transcurridos, límite permitido: %d segundos)",
                            segundosInactivo, limiteSegundos)
            );
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        loginEntity.setUltimaActividad(ahora);
        loginRepository.save(loginEntity);

        long segundosRestantes = Math.max(0, limiteSegundos - segundosInactivo);

        return LoginResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Sesión activa y vigente")
                .usuarioId(loginEntity.getUsuario() != null ? loginEntity.getUsuario().getId() : null)
                .username(username)
                .sesionActiva(true)
                .tiempoInactividadSegundos(limiteSegundos)
                .segundosRestantesInactividad(segundosRestantes)
                .ultimaActividad(ahora)
                .build();
    }

    @Override
    @Transactional
    public LoginResponse logout(String username) {
        log.info("Cerrando sesión para usuario: {}", username);
        String usernameCifrado = cryptoService.encrypt(username.trim());

        LoginEntity loginEntity = loginRepository.findByUsernameCifrado(usernameCifrado)
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario no registrado en el sistema"));

        loginEntity.setSesionActiva(false);
        loginRepository.save(loginEntity);
        log.info("Sesión cerrada exitosamente para usuario: {}", username);

        return LoginResponse.builder()
                .codigoRespuesta(CodigoRespuestaEnum.EXITO)
                .codigo(0)
                .mensaje("Cierre de sesión exitoso")
                .usuarioId(loginEntity.getUsuario() != null ? loginEntity.getUsuario().getId() : null)
                .username(username)
                .sesionActiva(false)
                .tiempoInactividadSegundos(loginEntity.getTiempoInactividadSegundos())
                .segundosRestantesInactividad(0L)
                .ultimaActividad(loginEntity.getUltimaActividad())
                .build();
    }
}
