package com.proyecto.servicios.security;

import com.proyecto.servicios.entity.cliente.LoginEntity;
import com.proyecto.servicios.repositorys.cliente.LoginRepository;
import com.proyecto.servicios.util.CryptoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas Unitarias - Migración del cifrado de login")
class MigracionCifradoLoginTest {

    private static final String LLAVE_HEREDADA = "LlaveHeredadaDePruebas";

    private final LoginRepository repositorio = mock(LoginRepository.class);
    private final CryptoService crypto = new CryptoService(LLAVE_HEREDADA, Base64.getEncoder().encodeToString(new byte[32]));
    private final MigracionCifradoLogin migracion = new MigracionCifradoLogin(repositorio, crypto);

    private static String cifrarComoAntes(String texto) throws Exception {
        byte[] llave = Arrays.copyOf(MessageDigest.getInstance("SHA-256").digest(LLAVE_HEREDADA.getBytes(StandardCharsets.UTF_8)), 16);
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(llave, "AES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(texto.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("Convierte usuario, 2FA y biometría, y el usuario se puede buscar por hash")
    void convierteTodosLosCampos() throws Exception {
        LoginEntity login = LoginEntity.builder()
                .usernameCifrado(cifrarComoAntes("ana"))
                .totpSecretoCifrado(cifrarComoAntes("SECRETO2FA"))
                .datosBiometricosCifrados(cifrarComoAntes("0.5,0.6"))
                .build();
        when(repositorio.findPendientesDeMigrarCifrado()).thenReturn(List.of(login));

        migracion.afterSingletonsInstantiated();

        assertEquals(crypto.hashBusqueda("ana"), login.getUsernameHash());
        assertTrue(login.getUsernameCifrado().startsWith("v2:"));
        assertEquals("ana", crypto.decrypt(login.getUsernameCifrado()));
        assertTrue(login.getTotpSecretoCifrado().startsWith("v2:"));
        assertEquals("SECRETO2FA", crypto.decrypt(login.getTotpSecretoCifrado()));
        assertEquals("0.5,0.6", crypto.decrypt(login.getDatosBiometricosCifrados()));
        verify(repositorio).save(login);
    }

    @Test
    @DisplayName("Respeta los campos vacíos")
    void camposOpcionalesVacios() throws Exception {
        LoginEntity login = LoginEntity.builder().usernameCifrado(cifrarComoAntes("ana")).build();
        when(repositorio.findPendientesDeMigrarCifrado()).thenReturn(List.of(login));

        migracion.afterSingletonsInstantiated();

        assertNull(login.getTotpSecretoCifrado());
        assertNull(login.getDatosBiometricosCifrados());
        verify(repositorio).save(login);
    }

    @Test
    @DisplayName("Si un registro no se puede leer, queda intacto y los demás sí se migran")
    void unRegistroDanadoNoDetieneLosDemas() throws Exception {
        LoginEntity danado = LoginEntity.builder().id(1L).usernameCifrado("esto-no-se-puede-descifrar").build();
        LoginEntity sano = LoginEntity.builder().id(2L).usernameCifrado(cifrarComoAntes("ana")).build();
        when(repositorio.findPendientesDeMigrarCifrado()).thenReturn(List.of(danado, sano));

        migracion.afterSingletonsInstantiated();

        assertEquals("esto-no-se-puede-descifrar", danado.getUsernameCifrado());
        assertNull(danado.getUsernameHash());
        verify(repositorio, never()).save(danado);
        verify(repositorio).save(sano);
        assertNotNull(sano.getUsernameHash());
    }

    @Test
    @DisplayName("Sin pendientes no guarda nada")
    void sinPendientes() {
        when(repositorio.findPendientesDeMigrarCifrado()).thenReturn(List.of());

        migracion.afterSingletonsInstantiated();

        verify(repositorio, never()).save(any());
    }
}
