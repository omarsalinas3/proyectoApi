package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.UsuarioDuplicadoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.service.BiometricService;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.service.CuentaService;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de la lógica de negocio para el Onboarding y ciclo de vida de Clientes Personas Físicas.
 *
 * <p><strong>Reglas de negocio garantizadas:</strong>
 * <ul>
 *   <li>Validación estricta de mayoría de edad (>= 18 años).</li>
 *   <li>Validación de ingresos mensuales superiores a $0.00.</li>
 *   <li>Unicidad de CURP, RFC, Correo y Nombre de usuario.</li>
 *   <li>Creación automática y atómica de cuenta de débito de 16 dígitos con saldo inicial en $0.00.</li>
 *   <li>Cifrado y hashing de credenciales y datos biométricos de alta precisión.</li>
 *   <li>Protección estricta de datos inmutables (CURP, RFC, Cuenta) en operaciones de actualización.</li>
 *   <li>Baja lógica con desactivación de cuenta y sesión.</li>
 * </ul>
 * </p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final UsuarioRepository usuarioRepository;
    private final LoginRepository loginRepository;
    private final CuentaService cuentaService;
    private final CryptoService cryptoService;
    private final BiometricService biometricService;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRegistroRequest request) {
        log.info("Iniciando proceso de Onboarding para cliente CURP: {}", request.getCurp());

        // 1. Validar mayoría de edad (>= 18 años)
        if (request.getFechaNacimiento() == null) {
            throw new ValidacionNegocioException("La fecha de nacimiento es obligatoria");
        }
        int edad = Period.between(request.getFechaNacimiento(), LocalDate.now()).getYears();
        if (edad < 18) {
            log.warn("Rechazo de registro: Cliente menor de edad ({} años). CURP: {}", edad, request.getCurp());
            throw new ValidacionNegocioException(String.format("El cliente debe ser mayor de edad (>= 18 años). Edad actual: %d años", edad));
        }

        // 2. Validar ingreso mensual mayor a cero
        if (request.getIngresoMensual() == null || request.getIngresoMensual().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionNegocioException("El ingreso mensual debe ser un valor positivo mayor a 0.00");
        }

        // 3. Validar unicidad de CURP, RFC, Correo y Usuario de acceso
        String curp = request.getCurp().trim().toUpperCase();
        String rfc = request.getRfc().trim().toUpperCase();
        String correo = request.getCorreo().trim().toLowerCase();
        String username = request.getUsername().trim();

        if (usuarioRepository.existsByCurp(curp)) {
            log.warn("Conflicto: La CURP {} ya está registrada", curp);
            throw new CurpDuplicadaException("La CURP ya se encuentra registrada en el sistema: " + curp);
        }

        if (usuarioRepository.existsByRfc(rfc)) {
            log.warn("Conflicto: El RFC {} ya está registrado", rfc);
            throw new RfcDuplicadoException("El RFC ya se encuentra registrado en el sistema: " + rfc);
        }

        if (usuarioRepository.existsByCorreo(correo)) {
            log.warn("Conflicto: El correo electrónico {} ya está registrado", correo);
            throw new CorreoDuplicadoException("El correo electrónico ya se encuentra registrado: " + correo);
        }

        String usernameCifrado = cryptoService.encrypt(username);
        if (loginRepository.existsByUsernameCifrado(usernameCifrado)) {
            log.warn("Conflicto: El nombre de usuario {} ya está registrado", username);
            throw new UsuarioDuplicadoException("El nombre de usuario ya se encuentra registrado: " + username);
        }

        // 4. Mapear y construir entidad de Usuario
        UsuarioEntity usuarioEntity = clienteMapper.requestToEntity(request);
        usuarioEntity.setCurp(curp);
        usuarioEntity.setRfc(rfc);
        usuarioEntity.setCorreo(correo);
        usuarioEntity.setActivo(true);

        // 5. Construir y asociar Domicilio
        DomicilioEntity domicilioEntity = clienteMapper.domicilioDtoToEntity(request.getDomicilio());
        domicilioEntity.setUsuario(usuarioEntity);
        usuarioEntity.setDomicilio(domicilioEntity);

        // 6. Generar automáticamente Cuenta Bancaria de 16 dígitos y Saldo en $0.00
        String numeroCuenta = cuentaService.generarNumeroCuentaUnico();
        CuentaEntity cuentaEntity = CuentaEntity.builder()
                .numeroCuenta(numeroCuenta)
                .estatus("ACTIVA")
                .saldoInicial(BigDecimal.ZERO)
                .saldoDisponible(BigDecimal.ZERO)
                .usuario(usuarioEntity)
                .build();

        SaldoEntity saldoEntity = SaldoEntity.builder()
                .usuario(usuarioEntity)
                .cuenta(cuentaEntity)
                .saldoActual(BigDecimal.ZERO)
                .saldoDisponible(BigDecimal.ZERO)
                .saldoRetenido(BigDecimal.ZERO)
                .moneda("MXN")
                .build();

        cuentaEntity.setSaldo(saldoEntity);
        usuarioEntity.setCuenta(cuentaEntity);

        // 7. Cifrar credenciales y datos biométricos MediaPipe (vector / escalar)
        String biometriaCifrada = biometricService.cifrarVectorEmbedding(
                request.getVectorEmbeddingFacial(),
                request.getDatosBiometricos()
        );

        LoginEntity loginEntity = LoginEntity.builder()
                .usuario(usuarioEntity)
                .usernameCifrado(usernameCifrado)
                .passwordHash(cryptoService.hashPassword(request.getPassword()))
                .datosBiometricosPrecision(request.getDatosBiometricos())
                .datosBiometricosCifrados(biometriaCifrada)
                .sesionActiva(false)
                .tiempoInactividadSegundos(300)
                .build();

        usuarioEntity.setLogin(loginEntity);

        // 8. Persistir de forma atómica
        UsuarioEntity savedUsuario = usuarioRepository.save(usuarioEntity);
        log.info("Onboarding completado exitosamente para cliente ID: {}, Cuenta: {}", savedUsuario.getId(), numeroCuenta);

        // 9. Construir respuesta estandarizada
        ClienteResponse response = clienteMapper.entityToResponse(savedUsuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente y cuenta bancaria registrados exitosamente");
        return response;
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request) {
        log.info("Actualizando información del cliente ID: {}", id);

        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        // 1. Validar que el nuevo correo no esté ocupado por otro cliente
        if (request.getCorreo() != null) {
            String nuevoCorreo = request.getCorreo().trim().toLowerCase();
            if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoAndIdNot(nuevoCorreo, id)) {
                log.warn("Conflicto: El correo {} ya está en uso por otro cliente", nuevoCorreo);
                throw new CorreoDuplicadoException("El correo electrónico ya se encuentra registrado por otro cliente: " + nuevoCorreo);
            }
        }

        // 2. Actualizar campos permitidos de la entidad
        clienteMapper.updateEntityFromRequest(request, usuario);
        if (request.getCorreo() != null) {
            usuario.setCorreo(request.getCorreo().trim().toLowerCase());
        }

        // 3. Actualizar datos de domicilio si vienen en la petición
        if (request.getDomicilio() != null && usuario.getDomicilio() != null) {
            clienteMapper.updateDomicilioFromDto(request.getDomicilio(), usuario.getDomicilio());
        }

        UsuarioEntity savedUsuario = usuarioRepository.save(usuario);
        log.info("Cliente ID: {} actualizado exitosamente", id);

        ClienteResponse response = clienteMapper.entityToResponse(savedUsuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Datos del cliente actualizados exitosamente");
        return response;
    }

    @Override
    @Transactional
    public ClienteResponse eliminarClienteLogico(Long id) {
        log.info("Ejecutando baja lógica para el cliente ID: {}", id);

        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        // Baja lógica del cliente
        usuario.setActivo(false);

        // Cancelación de cuenta bancaria asociada
        if (usuario.getCuenta() != null) {
            usuario.getCuenta().setEstatus("CANCELADA");
        }

        // Desactivación de sesiones activas
        if (usuario.getLogin() != null) {
            usuario.getLogin().setSesionActiva(false);
        }

        UsuarioEntity saved = usuarioRepository.save(usuario);
        log.info("Baja lógica aplicada al cliente ID: {}. Cuenta cancelada.", id);

        ClienteResponse response = clienteMapper.entityToResponse(saved);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente y productos asociados dados de baja correctamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorId(Long id) {
        log.info("Consultando cliente por ID: {}", id);
        UsuarioEntity usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con ID: " + id));

        ClienteResponse response = clienteMapper.entityToResponse(usuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente consultado exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorCurp(String curp) {
        log.info("Consultando cliente por CURP: {}", curp);
        UsuarioEntity usuario = usuarioRepository.findByCurp(curp.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con CURP: " + curp));

        ClienteResponse response = clienteMapper.entityToResponse(usuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente consultado exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorRfc(String rfc) {
        log.info("Consultando cliente por RFC: {}", rfc);
        UsuarioEntity usuario = usuarioRepository.findByRfc(rfc.trim().toUpperCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con RFC: " + rfc));

        ClienteResponse response = clienteMapper.entityToResponse(usuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente consultado exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorCorreo(String correo) {
        log.info("Consultando cliente por correo: {}", correo);
        UsuarioEntity usuario = usuarioRepository.findByCorreo(correo.trim().toLowerCase())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró el cliente con correo: " + correo));

        ClienteResponse response = clienteMapper.entityToResponse(usuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente consultado exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cliente por número de cuenta: {}", numeroCuenta);
        UsuarioEntity usuario = usuarioRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> new ClienteNoEncontradoException("No se encontró ningún cliente asociado a la cuenta: " + numeroCuenta));

        ClienteResponse response = clienteMapper.entityToResponse(usuario);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
        response.setMensaje("Cliente consultado exitosamente");
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> consultarTodos(Boolean soloActivos) {
        log.info("Consultando listado de clientes. Filtro soloActivos: {}", soloActivos);
        List<UsuarioEntity> usuarios = Boolean.TRUE.equals(soloActivos)
                ? usuarioRepository.findByActivoTrue()
                : usuarioRepository.findAll();

        return usuarios.stream()
                .map(u -> {
                    ClienteResponse r = clienteMapper.entityToResponse(u);
                    r.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
                    r.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> consultarPorRangoFechas(LocalDate inicio, LocalDate fin) {
        log.info("Consultando clientes por rango de fechas de registro: {} - {}", inicio, fin);
        OffsetDateTime fechaInicio = inicio.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fechaFin = fin.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);

        List<UsuarioEntity> usuarios = usuarioRepository.findByFechaRegistroBetween(fechaInicio, fechaFin);
        return usuarios.stream()
                .map(u -> {
                    ClienteResponse r = clienteMapper.entityToResponse(u);
                    r.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
                    r.setCodigo(CodigoRespuestaEnum.EXITO.getCodigo());
                    return r;
                })
                .collect(Collectors.toList());
    }
}
