package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.util.CryptoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Convierte al arrancar los datos de {@code login} guardados con el cifrado anterior: calcula el hash de búsqueda del
 * usuario y vuelve a cifrar usuario, secreto 2FA y biometría con AES-GCM.
 *
 * <p>Se ejecuta con {@link SmartInitializingSingleton} porque ocurre antes de que el servidor abra el puerto, así ninguna
 * petición de login llega a un registro sin convertir. Cada registro se guarda por separado: si uno falla se registra
 * solo su id, se conserva como estaba y se reintenta en el siguiente arranque. Es idempotente.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MigracionCifradoLogin implements SmartInitializingSingleton {

    private final LoginRepository loginRepository;
    private final CryptoService cryptoService;

    @Override
    public void afterSingletonsInstantiated() {
        List<LoginEntity> pendientes = loginRepository.findPendientesDeMigrarCifrado();
        if (pendientes.isEmpty()) {
            return;
        }
        log.info("Migrando el cifrado de {} registros de login", pendientes.size());
        int fallidos = 0;
        for (LoginEntity login : pendientes) {
            try {
                migrar(login);
                loginRepository.save(login);
            } catch (RuntimeException e) {
                fallidos++;
                log.error("No fue posible migrar el cifrado del login {}: {}", login.getId(), e.getClass().getSimpleName());
            }
        }
        if (fallidos > 0) {
            log.error("Migración de cifrado incompleta: {} de {} registros fallaron. Esos clientes no podrán iniciar "
                    + "sesión; revise que CRYPTO_SECRET_KEY sea la llave con la que se cifraron", fallidos, pendientes.size());
        } else {
            log.info("Migración de cifrado completada: {} registros", pendientes.size());
        }
    }

    private void migrar(LoginEntity login) {
        String usuario = cryptoService.decrypt(login.getUsernameCifrado());
        String hash = cryptoService.hashBusqueda(usuario);
        String usuarioCifrado = cryptoService.encrypt(usuario);
        String totp = recifrar(login.getTotpSecretoCifrado());
        String biometria = recifrar(login.getDatosBiometricosCifrados());

        // Se asigna al final: si algo falla arriba, la entidad queda sin cambios
        login.setUsernameHash(hash);
        login.setUsernameCifrado(usuarioCifrado);
        login.setTotpSecretoCifrado(totp);
        login.setDatosBiometricosCifrados(biometria);
    }

    private String recifrar(String valor) {
        return cryptoService.requiereMigracion(valor) ? cryptoService.encrypt(cryptoService.decrypt(valor)) : valor;
    }
}
