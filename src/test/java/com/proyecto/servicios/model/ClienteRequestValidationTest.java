package com.proyecto.servicios.model;

import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClienteRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private ClienteRegistroRequest.ClienteRegistroRequestBuilder valido() {
        return ClienteRegistroRequest.builder()
                .nombre("Carlos").apellidoPaterno("Hernández").apellidoMaterno("López")
                .fechaNacimiento(LocalDate.of(1995, 8, 15))
                .curp("HELC950815HDFRPR03").rfc("HELC950815ABC")
                .sexo("M").nacionalidad("Mexicana").estadoCivil("SOLTERO")
                .correo("carlos.hernandez@example.com").telefonoMovil("5512345678")
                .ocupacion("Ingeniero de Software").empresa("Tech Solutions S.A. de C.V.")
                .ingresoMensual(new BigDecimal("35000.00"))
                .domicilio(DomicilioDTO.builder().calle("Av. Juárez").numeroExterior("123")
                        .colonia("Centro").municipio("Guadalajara").estado("Jalisco")
                        .codigoPostal("44100").pais("México").build())
                .username("carlos.h95").password("PasswordSeguro123!");
    }

    private String mensajes(ClienteRegistroRequest r) {
        Set<ConstraintViolation<ClienteRegistroRequest>> v = validator.validate(r);
        return v.stream().map(c -> c.getPropertyPath() + ": " + c.getMessage()).sorted().toList().toString();
    }

    @Test
    void requestValidoNoGeneraErrores() {
        assertEquals(0, validator.validate(valido().build()).size(), mensajes(valido().build()));
    }

    @Test
    void nacionalidadRechazaNumerosYSimbolos() {
        String m = mensajes(valido().nacionalidad("M3xicana123").build());
        assertTrue(m.contains("nacionalidad: La nacionalidad solo debe contener letras"), m);
    }

    @Test
    void estadoCivilFueraDeCatalogoSeRechaza() {
        String m = mensajes(valido().estadoCivil("PIRATA").build());
        assertTrue(m.contains("estadoCivil: El estado civil no es válido"), m);
    }

    @Test
    void codigoPostalDebeTener5Digitos() {
        ClienteRegistroRequest r = valido().build();
        r.getDomicilio().setCodigoPostal("12AB");
        assertTrue(mensajes(r).contains("codigoPostal: El código postal debe contener exactamente 5 dígitos"));
    }

    @Test
    void passwordDebil() {
        assertTrue(mensajes(valido().password("password").build()).contains("password: La contraseña debe incluir"));
    }
}
