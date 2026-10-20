package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.RolEnum;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.security.JwtService;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.AuthService;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Implementación del servicio de autenticación y gestión de sesiones dinámicas.
 *
 * <p>El login emite un token JWT. Los mensajes de error de credenciales son genéricos para no revelar
 * si un usuario existe (enumeración de usuarios).</p>
 *
 * @author Equipo de Desarrollo
 * @version 2.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o credenciales incorrectas";
    private static final String TIPO_TOKEN = "Bearer";

    private final LoginRepository loginRepository;
    private final UsuarioRepository usuarioRepository;
    private final CryptoService cryptoService;
    private final JwtService jwtService;

    @Value("${seguridad.sesion.default-timeout-seconds:300}")
    private Integer defaultTimeoutSeconds;

    @Value("${seguridad.ejecutivo.usuario:ejecutivo}")
    private String ejecutivoUsuario;

    @Value("${seguridad.ejecutivo.password:}")
    private String ejecutivoPassword;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ValidacionNegocioException("El nombre de usuario es obligatorio");
        }

        String rawUsername = request.getUsername().trim();
        int bandera = request.getBandera() != null ? request.getBandera() : 1;
        log.info("Intento de inicio de sesión para el usuario: {}, Bandera: {}", rawUsername, bandera);

        if (bandera != 1 && bandera != 2) {
            throw new ValidacionNegocioException("Bandera de autenticación no soportada: " + bandera + ". Use 1 = Contraseña, 2 = Biométrico Face ID");
        }

        if (esLoginEjecutivo(rawUsername)) {
            return loginEjecutivo(request, bandera);
        }

        LoginEntity loginEntity = buscarLogin(rawUsername)
                .orElseThrow(() -> {
                    log.warn("Inicio de sesión fallido: usuario no registrado ({})", rawUsername);
                    return new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
                });

        if (bandera == 1) {
            // Bandera 1: Autenticación por contraseña
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new ValidacionNegocioException("La contraseña es obligatoria para la bandera 1 (Contraseña)");
            }
            if (!cryptoService.verifyPassword(request.getPassword(), loginEntity.getPasswordHash())) {
                log.warn("Contraseña incorrecta para el usuario: {}", rawUsername);
                throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
            }
            if (cryptoService.requiereMigracionHash(loginEntity.getPasswordHash())) {
                // Contraseñas guardadas con el hash anterior (SHA-256 sin sal) se migran a BCrypt al iniciar sesión.
                loginEntity.setPasswordHash(cryptoService.hashPassword(request.getPassword()));
                log.info("Hash de contraseña migrado a BCrypt para el usuario: {}", rawUsername);
            }
        } else {
            // Bandera 2: Autenticación por Face ID biométrico
            if (request.getFaceIdBiometrico() == null) {
                throw new ValidacionNegocioException("El identificador faceIdBiometrico es obligatorio para la bandera 2 (Biométrico Face ID)");
            }
            BigDecimal registeredPrecision = loginEntity.getDatosBiometricosPrecision();
            if (registeredPrecision == null) {
                log.warn("Intento de Face ID para usuario sin biometría registrada: {}", rawUsername);
                throw new CredencialesInvalidasException("El usuario no tiene un Face ID registrado; inicie sesión con contraseña (bandera 1)");
            }
            if (registeredPrecision.compareTo(BigDecimal.valueOf(request.getFaceIdBiometrico())) != 0) {
                log.warn("Fallo de coincidencia Face ID biométrico para el usuario: {}", rawUsername);
                throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
            }
            log.info("Autenticación biométrica Face ID exitosa para usuario: {}", rawUsername);
        }

        UsuarioEntity usuario = loginEntity.getUsuario();
        if (usuario != null && Boolean.FALSE.equals(usuario.getActivo())) {
            log.warn("Inicio de sesión rechazado: el cliente {} está dado de baja", usuario.getId());
            throw new CredencialesInvalidasException("El cliente se encuentra dado de baja y no puede iniciar sesión");
        }

        OffsetDateTime ahora = OffsetDateTime.now();
        int timeout = loginEntity.getTiempoInactividadSegundos() != null ? loginEntity.getTiempoInactividadSegundos() : defaultTimeout();

        loginEntity.setSesionActiva(true);
        loginEntity.setUltimaActividad(ahora);
        loginRepository.save(loginEntity);

        Long clienteId = usuario != null ? usuario.getId() : null;
        String token = jwtService.generarToken(new UsuarioAutenticado(clienteId, rawUsername, RolEnum.CLIENTE));
        log.info("Inicio de sesión exitoso para usuario: {}. Sesión activa = true", rawUsername);

        return LoginResponse.builder()
                .usuarioId(clienteId)
                .username(rawUsername)
                .rol(RolEnum.CLIENTE.name())
                .sesionActiva(true)
                .tiempoInactividadSegundos(timeout)
                .segundosRestantesInactividad((long) timeout)
                .ultimaActividad(ahora.toLocalDate())
                .token(token)
                .tipoToken(TIPO_TOKEN)
                .expiraEnSegundos(jwtService.getExpiracionSegundos())
                .build();
    }

    @Override
    @Transactional
    public LoginResponse verificarEstadoSesion(String username, Integer tiempoInactividadSegundos) {
        log.debug("Verificando estado de sesión para usuario: {}", username);
        LoginEntity loginEntity = buscarLogin(username.trim())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));
        return verificarSesion(loginEntity, username, tiempoInactividadSegundos);
    }

    @Override
    @Transactional
    public LoginResponse verificarEstadoSesion(UsuarioAutenticado usuario, Integer tiempoInactividadSegundos) {
        if (usuario.esEjecutivo()) {
            return respuestaEjecutivo(true);
        }
        LoginEntity loginEntity = loginRepository.findByUsuarioId(usuario.clienteId())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));
        return verificarSesion(loginEntity, usuario.username(), tiempoInactividadSegundos);
    }

    @Override
    @Transactional
    public LoginResponse logout(String username) {
        log.info("Cerrando sesión para usuario: {}", username);
        LoginEntity loginEntity = buscarLogin(username.trim())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));
        return cerrarSesion(loginEntity, username);
    }

    @Override
    @Transactional
    public LoginResponse logout(UsuarioAutenticado usuario) {
        if (usuario.esEjecutivo()) {
            return respuestaEjecutivo(false);
        }
        log.info("Cerrando sesión para usuario: {}", usuario.username());
        LoginEntity loginEntity = loginRepository.findByUsuarioId(usuario.clienteId())
                .orElseThrow(() -> new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS));
        return cerrarSesion(loginEntity, usuario.username());
    }

    @Override
    @Transactional
    public void validarSesionToken(Long clienteId) {
        LoginEntity loginEntity = loginRepository.findConUsuarioByUsuarioId(clienteId)
                .orElseThrow(() -> new CredencialesInvalidasException("El usuario del token ya no existe"));

        if (Boolean.FALSE.equals(loginEntity.getUsuario().getActivo())) {
            throw new SesionExpiradaException("El cliente fue dado de baja; el token ya no es válido");
        }
        if (!Boolean.TRUE.equals(loginEntity.getSesionActiva())) {
            throw new SesionExpiradaException("La sesión fue cerrada o expiró. Inicie sesión nuevamente en POST /api/v1/auth/login");
        }

        int limiteSegundos = loginEntity.getTiempoInactividadSegundos() != null
                ? loginEntity.getTiempoInactividadSegundos() : defaultTimeout();
        long segundosInactivo = segundosInactivo(loginEntity);
        if (segundosInactivo > limiteSegundos) {
            loginEntity.setSesionActiva(false);
            loginRepository.save(loginEntity);
            throw new SesionExpiradaException(String.format(
                    "La sesión ha expirado por inactividad (%d segundos transcurridos, límite permitido: %d segundos)",
                    segundosInactivo, limiteSegundos));
        }

        loginEntity.setUltimaActividad(OffsetDateTime.now());
        loginRepository.save(loginEntity);
    }

    // ─────────────────────────────
    // MÉTODOS PRIVADOS
    // ─────────────────────────────

    private Optional<LoginEntity> buscarLogin(String rawUsername) {
        Optional<LoginEntity> optLogin = loginRepository.findByUsernameCifrado(cryptoService.encrypt(rawUsername));
        if (optLogin.isPresent()) {
            return optLogin;
        }
        // Permite iniciar sesión con el correo o la CURP del cliente además del nombre de usuario
        Optional<UsuarioEntity> optUser = usuarioRepository.findByCorreo(rawUsername.toLowerCase());
        if (optUser.isEmpty()) {
            optUser = usuarioRepository.findByCurp(rawUsername.toUpperCase());
        }
        return optUser.map(UsuarioEntity::getLogin);
    }

    private LoginResponse verificarSesion(LoginEntity loginEntity, String username, Integer tiempoInactividadSegundos) {
        if (Boolean.FALSE.equals(loginEntity.getSesionActiva())) {
            log.warn("Intento de acceso con sesión inactiva para el usuario: {}", username);
            throw new SesionExpiradaException("La sesión no se encuentra activa");
        }

        int limiteSegundos = (tiempoInactividadSegundos != null && tiempoInactividadSegundos > 0)
                ? tiempoInactividadSegundos
                : (loginEntity.getTiempoInactividadSegundos() != null ? loginEntity.getTiempoInactividadSegundos() : defaultTimeout());

        long segundosInactivo = segundosInactivo(loginEntity);

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
                .usuarioId(loginEntity.getUsuario() != null ? loginEntity.getUsuario().getId() : null)
                .username(username)
                .rol(RolEnum.CLIENTE.name())
                .sesionActiva(true)
                .tiempoInactividadSegundos(limiteSegundos)
                .segundosRestantesInactividad(segundosRestantes)
                .ultimaActividad(ahora.toLocalDate())
                .build();
    }

    private LoginResponse cerrarSesion(LoginEntity loginEntity, String username) {
        loginEntity.setSesionActiva(false);
        loginRepository.save(loginEntity);
        log.info("Sesión cerrada exitosamente para usuario: {}", username);

        return LoginResponse.builder()
                .usuarioId(loginEntity.getUsuario() != null ? loginEntity.getUsuario().getId() : null)
                .username(username)
                .sesionActiva(false)
                .tiempoInactividadSegundos(loginEntity.getTiempoInactividadSegundos())
                .segundosRestantesInactividad(0L)
                .ultimaActividad(loginEntity.getUltimaActividad() != null ? loginEntity.getUltimaActividad().toLocalDate() : null)
                .build();
    }

    private long segundosInactivo(LoginEntity loginEntity) {
        OffsetDateTime ultimaActividad = loginEntity.getUltimaActividad() != null
                ? loginEntity.getUltimaActividad()
                : loginEntity.getFechaCreacion();
        return ultimaActividad != null ? Duration.between(ultimaActividad, OffsetDateTime.now()).getSeconds() : 0L;
    }

    private int defaultTimeout() {
        return defaultTimeoutSeconds != null ? defaultTimeoutSeconds : 300;
    }

    private boolean esLoginEjecutivo(String rawUsername) {
        return ejecutivoPassword != null && !ejecutivoPassword.isBlank()
                && ejecutivoUsuario != null && ejecutivoUsuario.equalsIgnoreCase(rawUsername);
    }

    private LoginResponse loginEjecutivo(LoginRequest request, int bandera) {
        if (bandera != 1) {
            throw new ValidacionNegocioException("El ejecutivo solo puede iniciar sesión con contraseña (bandera 1)");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ValidacionNegocioException("La contraseña es obligatoria para la bandera 1 (Contraseña)");
        }
        // Comparación en tiempo constante sobre los digest para no filtrar información por tiempos de respuesta
        if (!MessageDigest.isEqual(sha256(request.getPassword()), sha256(ejecutivoPassword))) {
            log.warn("Contraseña incorrecta para el usuario ejecutivo");
            throw new CredencialesInvalidasException(MENSAJE_CREDENCIALES_INVALIDAS);
        }
        String token = jwtService.generarToken(new UsuarioAutenticado(null, ejecutivoUsuario, RolEnum.EJECUTIVO));
        log.info("Inicio de sesión exitoso para el ejecutivo: {}", ejecutivoUsuario);

        LoginResponse response = respuestaEjecutivo(true);
        response.setToken(token);
        response.setTipoToken(TIPO_TOKEN);
        response.setExpiraEnSegundos(jwtService.getExpiracionSegundos());
        return response;
    }

    private LoginResponse respuestaEjecutivo(boolean sesionActiva) {
        return LoginResponse.builder()
                .username(ejecutivoUsuario)
                .rol(RolEnum.EJECUTIVO.name())
                .sesionActiva(sesionActiva)
                .build();
    }

    private static byte[] sha256(String valor) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
