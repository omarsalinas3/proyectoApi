package com.proyecto.servicios.security.gestopago;

import com.proyecto.servicios.exception.CifradoGestoPagoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - GestoPagoCifrador (Encryption Snippets)")
class GestoPagoCifradorTest {

    /** Par de prueba publicado en la documentación de GestoPago. */
    private static final String SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    /** Payload y "Encryption Expected Result" de la documentación. */
    private static final String PAYLOAD_DOCUMENTACION =
            "{'idProducto':582,'idServicio':133,'telefono':'5555555551','horaLocal':'20220101235900','upc':'test_UPC_83_182'}";
    private static final String RESULTADO_ESPERADO_DOCUMENTACION =
            "+LWZ7icI60/DAI8ggv/SpDvJpFYeaYOZ5J7hPGK7g/87r9PRJfgFRBoBLYpBxW33ouvuwQPnhhKHXqzbiqLhMSH4gSH8SMvW6PvHfeklNfTrZnbgWF"
                    + "WDPSy/nqFCP0B/gKavafHG0tW5UP4KPwyhECZK2pLT+I3qYAA85uQaAPQ=";

    private final GestoPagoCifrador cifrador = new GestoPagoCifrador(SECRET_KEY, IV);

    @Test
    @DisplayName("Cifrar el payload de la documentación produce exactamente el resultado esperado publicado")
    void cifradoIdenticoALaDocumentacion() {
        assertEquals(RESULTADO_ESPERADO_DOCUMENTACION, cifrador.cifrar(PAYLOAD_DOCUMENTACION));
    }

    @Test
    @DisplayName("Descifrar el resultado esperado de la documentación devuelve el payload original")
    void descifradoDelResultadoDocumentado() {
        assertEquals(PAYLOAD_DOCUMENTACION, cifrador.descifrar(RESULTADO_ESPERADO_DOCUMENTACION));
    }

    @Test
    @DisplayName("Ida y vuelta con caracteres UTF-8")
    void idaYVueltaUtf8() {
        String json = "{\"referencia\":\"Ñandú 25720381\",\"montoPago\":601.99}";
        assertEquals(json, cifrador.descifrar(cifrador.cifrar(json)));
    }

    @Test
    @DisplayName("Rechaza llaves que no son AES-256 o IV que no son de 16 bytes")
    void validaLlaveEIv() {
        assertThrows(IllegalStateException.class, () -> new GestoPagoCifrador("c2hvcnQ=", IV));
        assertThrows(IllegalStateException.class, () -> new GestoPagoCifrador(SECRET_KEY, "c2hvcnQ="));
        assertThrows(IllegalStateException.class, () -> new GestoPagoCifrador("no-es-base64!!", IV));
    }

    @Test
    @DisplayName("Un texto cifrado corrupto lanza CifradoGestoPagoException en lugar de devolver null")
    void cifradoCorrupto() {
        assertThrows(CifradoGestoPagoException.class, () -> cifrador.descifrar("AAAA"));
    }
}
