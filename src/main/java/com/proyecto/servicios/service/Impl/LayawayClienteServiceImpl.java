package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.enums.CodigoRespuestaEnum;
import com.proyecto.servicios.entity.cliente.CuentaEntity;
import com.proyecto.servicios.entity.cliente.DomicilioEntity;
import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.entity.cliente.SaldoEntity;
import com.proyecto.servicios.entity.cliente.UsuarioEntity;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CorreoDuplicadoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.mapper.ClienteMapper;
import com.proyecto.servicios.model.cliente.dto.DatosContactoDTO;
import com.proyecto.servicios.model.cliente.dto.DatosPersonalesDTO;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.model.cliente.request.LayawayClienteRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.repositorys.cliente.UsuarioRepository;
import com.proyecto.servicios.util.CryptoService;
import com.proyecto.servicios.service.LayawayClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;

/**
 * Implementación del servicio unificado de clientes (Layaway).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LayawayClienteServiceImpl implements LayawayClienteService {

    private final UsuarioRepository usuarioRepository;
    private final CryptoService cryptoService;
    private final ClienteMapper clienteMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public ClienteResponse procesarOperacionCliente(LayawayClienteRequest request) {
        if (request == null || request.getBandera() == null) {
            throw new ValidacionNegocioException("La bandera de operación es obligatoria (1 = Insertar, 2 = Actualizar, 3 = Eliminar)");
        }

        log.info("Procesando LayawayCliente con bandera: {}, clienteId: {}", request.getBandera(), request.getClienteId());

        return switch (request.getBandera()) {
            case 1 -> registrarCliente(request);
            case 2 -> actualizarCliente(request);
            case 3 -> eliminarCliente(request);
            default -> throw new ValidacionNegocioException(
                    "Bandera no soportada: " + request.getBandera() + ". Banderas válidas: 1 (Insertar), 2 (Actualizar), 3 (Eliminar/Baja Lógica)"
            );
        };
    }

    /**
     * Bandera 1: Registro completo y creación automática de cuenta bancaria.
     */
    private ClienteResponse registrarCliente(LayawayClienteRequest request) {
        DatosPersonalesDTO dp = request.getDatosPersonales();
        DatosContactoDTO dc = request.getDatosContacto();
        DomicilioDTO dom = request.getDomicilio();

        if (dp == null) {
            throw new ValidacionNegocioException("El objeto datosPersonales es obligatorio para la bandera 1 (Insertar)");
        }

        String rawCurp = dp.getCurp() != null ? safeDecrypt(dp.getCurp()) : "";
        String curp = rawCurp != null ? rawCurp.trim().toUpperCase() : "";
        String rawRfc = dp.getRfc() != null ? safeDecrypt(dp.getRfc()) : "";
        String rfc = rawRfc != null ? rawRfc.trim().toUpperCase() : "";
        String rawCorreo = (dc != null && dc.getCorreo() != null) ? safeDecrypt(dc.getCorreo()) : "";
        String correo = rawCorreo != null ? rawCorreo.trim().toLowerCase() : "";

        if (curp.isBlank() || curp.length() != 18) {
            throw new ValidacionNegocioException("La CURP es obligatoria y debe contener exactamente 18 caracteres (ej. HELC950815HDFRPR03)");
        }
        if (rfc.isBlank() || (rfc.length() != 12 && rfc.length() != 13)) {
            throw new ValidacionNegocioException("El RFC es obligatorio y debe contener 12 o 13 caracteres (ej. HELC950815ABC)");
        }

        if (!correo.isBlank() && !correo.matches("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            throw new ValidacionNegocioException("El correo electrónico '" + correo + "' no tiene un formato válido (ej. usuario@dominio.com)");
        }

        // Validación de fecha de nacimiento y mayoría de edad (18+ años)
        LocalDate fechaNac = dp.getFechaNacimiento() != null ? dp.getFechaNacimiento() : LocalDate.of(1990, 1, 1);
        if (fechaNac.isAfter(LocalDate.now())) {
            throw new ValidacionNegocioException("La fecha de nacimiento no puede ser una fecha futura");
        }
        if (java.time.Period.between(fechaNac, LocalDate.now()).getYears() < 18) {
            throw new ValidacionNegocioException("El cliente debe ser mayor de edad (18 años o más). Fecha ingresada: " + fechaNac);
        }

        if (usuarioRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException("Ya existe un cliente registrado con la CURP: " + curp);
        }
        if (usuarioRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException("Ya existe un cliente registrado con el RFC: " + rfc);
        }
        if (!correo.isBlank() && usuarioRepository.existsByCorreo(correo)) {
            throw new CorreoDuplicadoException("Ya existe un cliente registrado con el correo: " + correo);
        }

        String nombre = dp.getNombre() != null ? safeDecrypt(dp.getNombre()).trim() : "";
        String segundoNombre = dp.getSegundoNombre() != null ? safeDecrypt(dp.getSegundoNombre()).trim() : null;
        String apellidoPaterno = dp.getApellidoPaterno() != null ? safeDecrypt(dp.getApellidoPaterno()).trim() : "";
        String apellidoMaterno = dp.getApellidoMaterno() != null ? safeDecrypt(dp.getApellidoMaterno()).trim() : "";

        // Validación de solo texto en nombres y apellidos
        if (nombre.isBlank() || !nombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            throw new ValidacionNegocioException("El nombre es obligatorio y solo debe contener letras y espacios");
        }
        if (segundoNombre != null && !segundoNombre.isBlank() && !segundoNombre.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            throw new ValidacionNegocioException("El segundo nombre solo debe contener letras y espacios");
        }
        if (apellidoPaterno.isBlank() || !apellidoPaterno.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            throw new ValidacionNegocioException("El apellido paterno es obligatorio y solo debe contener letras y espacios");
        }
        if (apellidoMaterno.isBlank() || !apellidoMaterno.matches("^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$")) {
            throw new ValidacionNegocioException("El apellido materno es obligatorio y solo debe contener letras y espacios");
        }

        // Validación de teléfono (solo números, 10 dígitos)
        String telMovil = (dc != null && dc.getTelefonoMovil() != null) ? safeDecrypt(dc.getTelefonoMovil()).trim() : "";
        if (!telMovil.isBlank() && !telMovil.matches("^\\d{10}$")) {
            throw new ValidacionNegocioException("El teléfono móvil '" + telMovil + "' no es válido. Debe contener exactamente 10 dígitos numéricos");
        }

        // Validación de código postal (solo números, 5 dígitos)
        String cp = (dom != null && dom.getCodigoPostal() != null) ? safeDecrypt(dom.getCodigoPostal()).trim() : "00000";
        if (!cp.isBlank() && !cp.matches("^\\d{5}$")) {
            throw new ValidacionNegocioException("El código postal '" + cp + "' no es válido. Debe contener exactamente 5 dígitos numéricos");
        }

        BigDecimal ceroDecimal = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);

        String ocupacion = (request.getInformacionFinanciera() != null && request.getInformacionFinanciera().getOcupacion() != null)
                ? request.getInformacionFinanciera().getOcupacion().trim() : "Cliente General";
        BigDecimal ingreso = (request.getInformacionFinanciera() != null && request.getInformacionFinanciera().getIngresoMensual() != null)
                ? request.getInformacionFinanciera().getIngresoMensual().setScale(2, java.math.RoundingMode.HALF_UP) : ceroDecimal;

        UsuarioEntity usuario = UsuarioEntity.builder()
                .nombre(nombre)
                .segundoNombre(segundoNombre)
                .apellidoPaterno(apellidoPaterno)
                .apellidoMaterno(apellidoMaterno)
                .fechaNacimiento(dp.getFechaNacimiento() != null ? dp.getFechaNacimiento() : LocalDate.of(1990, 1, 1))
                .curp(curp)
                .rfc(rfc)
                .sexo(com.proyecto.servicios.enums.SexoEnum.normalizar(dp.getSexo() != null ? safeDecrypt(dp.getSexo()) : null))
                .nacionalidad(dp.getNacionalidad() != null ? safeDecrypt(dp.getNacionalidad()).trim() : "Mexicana")
                .estadoCivil(com.proyecto.servicios.enums.EstadoCivilEnum.normalizar(dp.getEstadoCivil() != null ? safeDecrypt(dp.getEstadoCivil()) : null))
                .correo(!correo.isBlank() ? correo : (curp.toLowerCase() + "@layaway.com"))
                .telefonoMovil(!telMovil.isBlank() ? telMovil : "0000000000")
                .telefonoAlternativo(dc != null && dc.getTelefonoAlt() != null ? safeDecrypt(dc.getTelefonoAlt()).trim() : null)
                .ocupacion(ocupacion)
                .empresa("Particular")
                .ingresoMensual(ingreso)
                .activo(true)
                .build();

        if (dom != null) {
            DomicilioEntity domicilioEntity = DomicilioEntity.builder()
                    .calle(dom.getCalle() != null ? dom.getCalle() : "No especificada")
                    .numeroExterior(dom.getNumeroExterior() != null ? dom.getNumeroExterior() : "S/N")
                    .numeroInterior(dom.getNumeroInterior())
                    .colonia(dom.getColonia() != null ? dom.getColonia() : "Centro")
                    .municipio(dom.getMunicipio() != null ? dom.getMunicipio() : "Principal")
                    .estado(dom.getEstado() != null ? dom.getEstado() : "CDMX")
                    .codigoPostal(!cp.isBlank() ? cp : "00000")
                    .pais(dom.getPais() != null ? dom.getPais() : "México")
                    .usuario(usuario)
                    .build();
            usuario.setDomicilio(domicilioEntity);
        }

        // Generar Cuenta Bancaria de 16 dígitos
        String numeroCuenta = "4152" + String.format("%012d", Math.abs(secureRandom.nextLong() % 1_000_000_000_000L));
        CuentaEntity cuenta = CuentaEntity.builder()
                .usuario(usuario)
                .numeroCuenta(numeroCuenta)
                .saldoInicial(ceroDecimal)
                .saldoDisponible(ceroDecimal)
                .estatus("ACTIVA")
                .build();

        SaldoEntity saldo = SaldoEntity.builder()
                .usuario(usuario)
                .cuenta(cuenta)
                .saldoActual(ceroDecimal)
                .saldoRetenido(ceroDecimal)
                .saldoDisponible(ceroDecimal)
                .moneda("MXN")
                .build();

        cuenta.setSaldo(saldo);
        usuario.setCuenta(cuenta);

        // Generar Credenciales y Biometría
        String rawUsername = !correo.isBlank() ? correo : curp;
        String userCifrado = cryptoService.encrypt(rawUsername);

        String plainPassword = curp;
        if (request.getAutenticacion() != null && request.getAutenticacion().getPassword() != null && !request.getAutenticacion().getPassword().isBlank()) {
            plainPassword = safeDecrypt(request.getAutenticacion().getPassword()).trim();
        }
        String passHash = cryptoService.hashPassword(plainPassword);

        Long rawBio = null;
        if (request.getAutenticacion() != null && request.getAutenticacion().getFaceId() != null) {
            rawBio = request.getAutenticacion().getFaceId();
        } else if (dp.getDatosBiometricos() != null) {
            rawBio = dp.getDatosBiometricos();
        }

        BigDecimal bioPrecision = rawBio != null ? BigDecimal.valueOf(rawBio) : null;
        String bioCifrado = rawBio != null ? cryptoService.encrypt(String.valueOf(rawBio)) : null;

        LoginEntity login = LoginEntity.builder()
                .usuario(usuario)
                .usernameCifrado(userCifrado)
                .passwordHash(passHash)
                .datosBiometricosPrecision(bioPrecision)
                .datosBiometricosCifrados(bioCifrado)
                .sesionActiva(false)
                .tiempoInactividadSegundos(300)
                .build();

        usuario.setLogin(login);

        UsuarioEntity saved = usuarioRepository.save(usuario);
        log.info("Cliente registrado con éxito ID: {}, Cuenta: {}", saved.getId(), numeroCuenta);

        ClienteResponse response = clienteMapper.entityToResponse(saved);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(0);
        response.setMensaje("Cliente registrado y cuenta bancaria generada exitosamente (Bandera 1)");
        return response;
    }

    /**
     * Bandera 2: Actualización de datos protegiendo CURP/RFC.
     */
    private ClienteResponse actualizarCliente(LayawayClienteRequest request) {
        if (request.getClienteId() == null || request.getClienteId() <= 0) {
            throw new ValidacionNegocioException("El clienteId es obligatorio para la bandera 2 (Actualizar)");
        }

        UsuarioEntity usuario = usuarioRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente con ID " + request.getClienteId() + " no encontrado"));

        DatosPersonalesDTO dp = request.getDatosPersonales();
        if (dp != null) {
            if (dp.getNombre() != null && !dp.getNombre().isBlank()) usuario.setNombre(dp.getNombre().trim());
            if (dp.getSegundoNombre() != null) usuario.setSegundoNombre(dp.getSegundoNombre().trim());
            if (dp.getApellidoPaterno() != null && !dp.getApellidoPaterno().isBlank()) usuario.setApellidoPaterno(dp.getApellidoPaterno().trim());
            if (dp.getApellidoMaterno() != null && !dp.getApellidoMaterno().isBlank()) usuario.setApellidoMaterno(dp.getApellidoMaterno().trim());
            if (dp.getFechaNacimiento() != null) usuario.setFechaNacimiento(dp.getFechaNacimiento());
            if (dp.getSexo() != null) usuario.setSexo(com.proyecto.servicios.enums.SexoEnum.normalizar(safeDecrypt(dp.getSexo())));
            if (dp.getNacionalidad() != null) usuario.setNacionalidad(safeDecrypt(dp.getNacionalidad()).trim());
            if (dp.getEstadoCivil() != null) usuario.setEstadoCivil(com.proyecto.servicios.enums.EstadoCivilEnum.normalizar(safeDecrypt(dp.getEstadoCivil())));
            if (dp.getDatosBiometricos() != null && usuario.getLogin() != null) {
                usuario.getLogin().setDatosBiometricosPrecision(BigDecimal.valueOf(dp.getDatosBiometricos()));
                usuario.getLogin().setDatosBiometricosCifrados(cryptoService.encrypt(String.valueOf(dp.getDatosBiometricos())));
            }
        }

        DatosContactoDTO dc = request.getDatosContacto();
        if (dc != null) {
            if (dc.getCorreo() != null && !dc.getCorreo().isBlank()) {
                String nuevoCorreo = dc.getCorreo().trim().toLowerCase();
                if (!nuevoCorreo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreo(nuevoCorreo)) {
                    throw new CorreoDuplicadoException("El correo " + nuevoCorreo + " ya pertenece a otro cliente");
                }
                usuario.setCorreo(nuevoCorreo);
            }
            if (dc.getTelefonoMovil() != null && !dc.getTelefonoMovil().isBlank()) {
                usuario.setTelefonoMovil(dc.getTelefonoMovil().trim());
            }
            if (dc.getTelefonoAlt() != null) {
                usuario.setTelefonoAlternativo(dc.getTelefonoAlt().trim());
            }
        }

        DomicilioDTO dom = request.getDomicilio();
        if (dom != null) {
            DomicilioEntity domEntity = usuario.getDomicilio();
            if (domEntity == null) {
                domEntity = DomicilioEntity.builder().usuario(usuario).build();
                usuario.setDomicilio(domEntity);
            }
            if (dom.getCalle() != null) domEntity.setCalle(dom.getCalle());
            if (dom.getNumeroExterior() != null) domEntity.setNumeroExterior(dom.getNumeroExterior());
            if (dom.getNumeroInterior() != null) domEntity.setNumeroInterior(dom.getNumeroInterior());
            if (dom.getColonia() != null) domEntity.setColonia(dom.getColonia());
            if (dom.getMunicipio() != null) domEntity.setMunicipio(dom.getMunicipio());
            if (dom.getEstado() != null) domEntity.setEstado(dom.getEstado());
            if (dom.getCodigoPostal() != null) domEntity.setCodigoPostal(dom.getCodigoPostal());
            if (dom.getPais() != null) domEntity.setPais(dom.getPais());
        }

        UsuarioEntity updated = usuarioRepository.save(usuario);
        log.info("Cliente actualizado con éxito ID: {}", updated.getId());

        ClienteResponse response = clienteMapper.entityToResponse(updated);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(0);
        response.setMensaje("Cliente actualizado exitosamente preservando CURP/RFC (Bandera 2)");
        return response;
    }

    /**
     * Bandera 3: Baja lógica y cancelación de cuenta.
     */
    private ClienteResponse eliminarCliente(LayawayClienteRequest request) {
        if (request.getClienteId() == null || request.getClienteId() <= 0) {
            throw new ValidacionNegocioException("El clienteId es obligatorio para la bandera 3 (Eliminar/Baja Lógica)");
        }

        UsuarioEntity usuario = usuarioRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException("Cliente con ID " + request.getClienteId() + " no encontrado"));

        usuario.setActivo(false);
        if (usuario.getCuenta() != null) {
            usuario.getCuenta().setEstatus("CANCELADA");
        }
        if (usuario.getLogin() != null) {
            usuario.getLogin().setSesionActiva(false);
        }

        UsuarioEntity deactivated = usuarioRepository.save(usuario);
        log.info("Cliente dado de baja lógica ID: {}", deactivated.getId());

        ClienteResponse response = clienteMapper.entityToResponse(deactivated);
        response.setCodigoRespuesta(CodigoRespuestaEnum.EXITO);
        response.setCodigo(0);
        response.setMensaje("Cliente dado de baja lógica exitosamente (Bandera 3)");
        return response;
    }

    /**
     * Descifra de forma segura cadenas que vengan en Base64/AES, o retorna el texto claro si no está cifrado.
     */
    private String safeDecrypt(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        try {
            return cryptoService.decrypt(text.trim());
        } catch (Exception e) {
            return text.trim();
        }
    }
}
