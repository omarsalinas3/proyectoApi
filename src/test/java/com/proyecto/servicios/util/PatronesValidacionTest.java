package com.proyecto.servicios.util;

import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Expresiones de validación")
class PatronesValidacionTest {

    private static final Duration LIMITE = Duration.ofSeconds(2);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static boolean coincide(String regex, String texto) {
        return Pattern.compile(regex).matcher(texto).matches();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Mexicana", "Costa Rica", "Peruana", "Ñandú"})
    void nacionalidadValida(String texto) {
        assertTrue(coincide(PatronesValidacion.NACIONALIDAD, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Costa  Rica", " Mexicana", "Mexicana ", "Mexicana1", ""})
    void nacionalidadInvalida(String texto) {
        assertFalse(coincide(PatronesValidacion.NACIONALIDAD, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ingeniero", "Ingeniero de Software, Sr.", "Médico-cirujano"})
    void ocupacionValida(String texto) {
        assertTrue(coincide(PatronesValidacion.OCUPACION, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Ingeniero 3", " Ingeniero", ".Ingeniero", ""})
    void ocupacionInvalida(String texto) {
        assertFalse(coincide(PatronesValidacion.OCUPACION, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@dominio.com", "ana.p+1@mi-dominio.com.mx", "a_b%c@x.io"})
    void correoValido(String texto) {
        assertTrue(coincide(PatronesValidacion.CORREO, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ana@dominio", "ana@dominio.c", "ana@dominio.c0m", "@dominio.com", "ana@.com", "ana dominio.com"})
    void correoInvalido(String texto) {
        assertFalse(coincide(PatronesValidacion.CORREO, texto));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SOLTERO", "casada", "UNION_LIBRE", "union libre", "UNIÓN LIBRE", "unión libre", "UNIÓN_LIBRE", "viuda"})
    void estadoCivilValido(String texto) {
        assertTrue(Pattern.compile(PatronesValidacion.ESTADO_CIVIL, Pattern.CANON_EQ).matcher(texto).matches());
    }

    @Test
    @DisplayName("Texto de 1 millón de caracteres: se responde de inmediato en lugar de congelar la validación")
    void textosLargosNoCongelan() {
        assertTimeoutPreemptively(LIMITE,
                () -> assertFalse(coincide(PatronesValidacion.OCUPACION, "a".repeat(1_000_000) + "1")));
        assertTimeoutPreemptively(LIMITE,
                () -> assertFalse(coincide(PatronesValidacion.NACIONALIDAD, "a ".repeat(500_000) + "1")));
        assertTimeoutPreemptively(LIMITE,
                () -> assertFalse(coincide(PatronesValidacion.CORREO, "a@" + "a.".repeat(500_000) + "1")));
    }

    @Test
    @DisplayName("La validación real del registro responde rápido con campos enormes y los rechaza")
    void registroConCamposEnormes() {
        ClienteRegistroRequest request = new ClienteRegistroRequest();
        request.setOcupacion("a".repeat(200_000) + "1");
        request.setNacionalidad("a ".repeat(100_000) + "1");
        request.setCorreo("a@" + "a.".repeat(100_000) + "1");

        var errores = assertTimeoutPreemptively(Duration.ofSeconds(5), () -> VALIDATOR.validate(request));

        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("ocupacion")));
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("nacionalidad")));
        assertTrue(errores.stream().anyMatch(e -> e.getPropertyPath().toString().equals("correo")));
    }
}
