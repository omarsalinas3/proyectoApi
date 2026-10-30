package com.proyecto.servicios.util;

import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Ocultar credenciales en los logs")
class OcultadorDatosSensiblesTest {

    @Test
    @DisplayName("Oculta la contraseña de la URL de autenticación de GestoPago y conserva el resto")
    void ocultaPasswordEnUrl() {
        String mensaje = "Connection refused executing POST https://gestopago.portalventas.net/sistema/app/jwt-gp/authenticate/"
                + "?idDistribuidor=83&codigoDispositivo=GPS83-TPV-17&password=12345678";

        String limpio = OcultadorDatosSensibles.ocultar(mensaje);

        assertFalse(limpio.contains("12345678"), limpio);
        assertTrue(limpio.contains("password=****"), limpio);
        assertTrue(limpio.contains("idDistribuidor=83&codigoDispositivo=GPS83-TPV-17"), limpio);
    }

    @Test
    @DisplayName("Oculta tokens Bearer, api keys y el payload cifrado")
    void ocultaTokensYPayload() {
        String limpio = OcultadorDatosSensibles.ocultar(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.abc.def== api_key=XYZ123 signed=AbC+/9== random=1761");

        assertEquals("Authorization: Bearer **** api_key=**** signed=**** random=1761", limpio);
    }

    @Test
    @DisplayName("describir() usa el mensaje de Feign ya sin la contraseña")
    void describeExcepcionDeFeign() {
        RetryableException ex = new RetryableException(-1,
                "Connection refused executing POST https://h/authenticate/?password=secreta", Request.HttpMethod.POST,
                new ConnectException("Connection refused"), (Long) null,
                Request.create(Request.HttpMethod.POST, "https://h", Map.of(), new byte[0], StandardCharsets.UTF_8, null));

        String descripcion = OcultadorDatosSensibles.describir(ex);

        assertTrue(descripcion.startsWith("RetryableException: "), descripcion);
        assertFalse(descripcion.contains("secreta"), descripcion);
        assertNull(OcultadorDatosSensibles.ocultar(null));
    }
}
