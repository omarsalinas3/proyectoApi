package com.proyecto.servicios.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - URL de base de datos con credenciales")
class ConfigDBTest {

    private static Matcher separar(String url) {
        return ConfigDB.EMBEDDED_CREDENTIALS_URL.matcher(url);
    }

    @Test
    @DisplayName("Formato de Render: usuario, contraseña, host y base sin puerto")
    void urlDeRender() {
        Matcher m = separar("postgresql://usuario_db:clave123@dpg-abc-a/mi_base");

        assertTrue(m.matches());
        assertEquals("usuario_db", m.group(1));
        assertEquals("clave123", m.group(2));
        assertEquals("dpg-abc-a", m.group(3));
        assertNull(m.group(4));
        assertEquals("/mi_base", m.group(5));
        assertNull(m.group(6));
    }

    @Test
    @DisplayName("La contraseña puede contener @ y dos puntos; se respeta puerto y query")
    void contrasenaConCaracteresEspeciales() {
        Matcher m = separar("jdbc:postgresql://u:p@ss:w0rd@host.render.com:5432/base?sslmode=require");

        assertTrue(m.matches());
        assertEquals("p@ss:w0rd", m.group(2));
        assertEquals("host.render.com", m.group(3));
        assertEquals("5432", m.group(4));
        assertEquals("/base", m.group(5));
        assertEquals("?sslmode=require", m.group(6));
    }

    @Test
    @DisplayName("Una URL JDBC sin credenciales no se modifica")
    void urlSinCredenciales() {
        assertFalse(separar("jdbc:postgresql://localhost:5432/pago_servicios_db").matches());
        assertFalse(separar("jdbc:h2:mem:prueba").matches());
    }
}
