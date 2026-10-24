package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ExternalServiceException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - GestoPagoXmlParser (respuestas de la documentación)")
class GestoPagoXmlParserTest {

    private final GestoPagoXmlParser parser = new GestoPagoXmlParser();

    @Test
    @DisplayName("sendTx exitoso (CODIGO 01)")
    void sendTxExitoso() {
        GestoPagoTxResultado r = parser.parsear("""
                <?xml version='1.0' encoding='UTF-8'?>
                <RESPONSE>
                    <ID_TX>291437188</ID_TX>
                    <NUM_AUTORIZACION>100024988</NUM_AUTORIZACION>
                    <SALDO>157731.83</SALDO>
                    <COMISION>0.7</COMISION>
                    <SALDO_F>157,731.83</SALDO_F>
                    <COMISION_F>0.7</COMISION_F>
                    <FECHA>25/Feb/2022 18:15:25</FECHA>
                    <MONTO>10.0</MONTO>
                    <MENSAJE>
                        <CODIGO>01</CODIGO>
                        <TEXTO>Operacion realizada con exito</TEXTO>
                        <REFERENCIA></REFERENCIA>
                    </MENSAJE>
                </RESPONSE>""");

        assertTrue(r.esExitosa());
        assertEquals(291437188L, r.getIdTx());
        assertEquals("100024988", r.getNumAutorizacion());
        assertEquals(new BigDecimal("0.7"), r.getComision());
        assertEquals("25/Feb/2022 18:15:25", r.getFecha());
        assertNull(r.getReferencia());
    }

    @Test
    @DisplayName("sendTx saldo insuficiente del distribuidor (CODIGO 02) y timeout (CODIGO 82)")
    void sendTxErrores() {
        GestoPagoTxResultado saldo = parser.parsear("<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE>"
                + "<CODIGO>02</CODIGO><TEXTO>Saldo insuficiente</TEXTO></MENSAJE></RESPONSE>");
        assertFalse(saldo.esExitosa());
        assertEquals("02", saldo.getCodigo());

        GestoPagoTxResultado timeout = parser.parsear("<RESPONSE><NUM_AUTORIZACION>-1</NUM_AUTORIZACION><MENSAJE>"
                + "<CODIGO>82</CODIGO><TEXTO>Tiempo de espera alcanzado, la transacción no ha sido registrada</TEXTO></MENSAJE></RESPONSE>");
        assertTrue(timeout.esTiempoDeEsperaAgotado());
    }

    @Test
    @DisplayName("confirmTx aplicada (CODIGO 06 con autorización) y no aplicada (CODIGO 70)")
    void confirmTx() {
        GestoPagoTxResultado aplicada = parser.parsear("<RESPONSE><NUM_AUTORIZACION>100024988</NUM_AUTORIZACION><MENSAJE>"
                + "<CODIGO>06</CODIGO><TEXTO>La transaccion fue aplicada exitosamente.</TEXTO><ID_TX>291437188</ID_TX>"
                + "</MENSAJE></RESPONSE>");
        assertTrue(aplicada.confirmaAplicada());
        assertEquals(291437188L, aplicada.getIdTx());

        GestoPagoTxResultado noAplicada = parser.parsear("<RESPONSE><NUM_AUTORIZACION>0</NUM_AUTORIZACION><MENSAJE>"
                + "<CODIGO>70</CODIGO><TEXTO>La transaccion no fue aplicada</TEXTO><ID_TX>-1</ID_TX></MENSAJE></RESPONSE>");
        assertFalse(noAplicada.confirmaAplicada());
        assertEquals(GestoPagoTxResultado.CODIGO_NO_APLICADA, noAplicada.getCodigo());
    }

    @Test
    @DisplayName("SALDO en notación científica se interpreta correctamente")
    void saldoNotacionCientifica() {
        GestoPagoTxResultado r = parser.parsear("<RESPONSE><SALDO>1.5773183E5</SALDO><MENSAJE><CODIGO>01</CODIGO></MENSAJE></RESPONSE>");
        assertEquals(0, new BigDecimal("157731.83").compareTo(r.getSaldo()));
    }

    @Test
    @DisplayName("Rechaza respuestas vacías, sin CODIGO o con DOCTYPE (protección XXE)")
    void respuestasInvalidas() {
        assertThrows(ExternalServiceException.class, () -> parser.parsear(""));
        assertThrows(ExternalServiceException.class, () -> parser.parsear("<RESPONSE><MENSAJE/></RESPONSE>"));
        assertThrows(ExternalServiceException.class, () -> parser.parsear("no es xml"));
        assertThrows(ExternalServiceException.class, () -> parser.parsear(
                "<?xml version=\"1.0\"?><!DOCTYPE r [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                        + "<RESPONSE><MENSAJE><CODIGO>&x;</CODIGO></MENSAJE></RESPONSE>"));
    }
}
