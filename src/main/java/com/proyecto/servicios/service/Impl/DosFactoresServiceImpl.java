package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.DosFactoresInvalidoException;
import com.proyecto.servicios.exception.DosFactoresNoConfiguradoException;
import com.proyecto.servicios.exception.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.response.DosFactoresResponse;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.security.TotpGenerador;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.DosFactoresService;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.OptionalLong;

/**
 * Implementación del segundo factor (TOTP). El secreto se guarda cifrado y cada código se acepta una sola vez.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DosFactoresServiceImpl implements DosFactoresService {

    private static final String MENSAJE_CODIGO_INVALIDO =
            "El código de verificación es incorrecto, expiró o ya fue utilizado; use el código vigente de su app de autenticación";

    private final LoginRepository loginRepository;
    private final CryptoService cryptoService;
    private final TotpGenerador totpGenerador;
    private final Clock clock;

    @Value("${seguridad.dos-factores.emisor:ProyectoApi}")
    private String emisor;

    @Override
    @Transactional
    public DosFactoresResponse iniciarActivacion(UsuarioAutenticado usuario) {
        LoginEntity login = loginDe(usuario);
        if (Boolean.TRUE.equals(login.getTotpActivo())) {
            throw new ValidacionNegocioException("El segundo factor ya está activo para este cliente");
        }
        String secreto = totpGenerador.generarSecreto();
        login.setTotpSecretoCifrado(cryptoService.encrypt(secreto));
        login.setTotpUltimoPaso(null);
        loginRepository.save(login);
        log.info("Activación de 2FA iniciada para el cliente {}", usuario.clienteId());
        return DosFactoresResponse.builder()
                .activo(false)
                .secreto(secreto)
                .uriProvisionamiento(totpGenerador.uriProvisionamiento(emisor, usuario.username(), secreto))
                .build();
    }

    @Override
    @Transactional
    public DosFactoresResponse confirmarActivacion(UsuarioAutenticado usuario, String codigo) {
        LoginEntity login = loginDe(usuario);
        if (Boolean.TRUE.equals(login.getTotpActivo())) {
            throw new ValidacionNegocioException("El segundo factor ya está activo para este cliente");
        }
        if (login.getTotpSecretoCifrado() == null) {
            throw new DosFactoresNoConfiguradoException(
                    "Primero inicie la activación del segundo factor en POST /api/v1/auth/2fa/activar");
        }
        verificarYConsumir(login, codigo);
        login.setTotpActivo(true);
        loginRepository.save(login);
        log.info("2FA activado para el cliente {}", usuario.clienteId());
        return DosFactoresResponse.builder().activo(true).build();
    }

    @Override
    @Transactional
    public void validarCodigo(Long clienteId, String codigo) {
        LoginEntity login = loginRepository.findByUsuarioId(clienteId)
                .orElseThrow(() -> new CredencialesInvalidasException("El cliente no tiene credenciales registradas"));
        if (!Boolean.TRUE.equals(login.getTotpActivo()) || login.getTotpSecretoCifrado() == null) {
            throw new DosFactoresNoConfiguradoException("Active el segundo factor antes de operar: POST /api/v1/auth/2fa/activar "
                    + "y luego POST /api/v1/auth/2fa/confirmar");
        }
        verificarYConsumir(login, codigo);
        loginRepository.save(login);
    }

    private void verificarYConsumir(LoginEntity login, String codigo) {
        String secreto = cryptoService.decrypt(login.getTotpSecretoCifrado());
        OptionalLong paso = totpGenerador.verificar(secreto, codigo, login.getTotpUltimoPaso(), clock.instant());
        if (paso.isEmpty()) {
            log.warn("Código 2FA rechazado para el cliente {}", login.getUsuario() != null ? login.getUsuario().getId() : null);
            throw new DosFactoresInvalidoException(MENSAJE_CODIGO_INVALIDO);
        }
        login.setTotpUltimoPaso(paso.getAsLong());
    }

    private LoginEntity loginDe(UsuarioAutenticado usuario) {
        if (usuario.esEjecutivo()) {
            throw new AccesoDenegadoException("El segundo factor de transacciones aplica solo a clientes");
        }
        return loginRepository.findByUsuarioId(usuario.clienteId())
                .orElseThrow(() -> new CredencialesInvalidasException("El cliente no tiene credenciales registradas"));
    }
}
