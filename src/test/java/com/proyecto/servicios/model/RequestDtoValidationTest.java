package com.proyecto.servicios.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.ClienteIdRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.LayawayClienteRequest;
import com.proyecto.servicios.model.cliente.request.LoginRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Validaciones declarativas de los Request DTO")
class RequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** Errores en el mismo formato que devuelve la API: campo → mensaje. */
    private <T> Map<String, String> errores(T dto) {
        Map<String, String> errores = new TreeMap<>();
        for (ConstraintViolation<T> v : validator.validate(dto)) {
            errores.merge(v.getPropertyPath().toString(), v.getMessage(), (a, b) -> a + "; " + b);
        }
        return errores;
    }

    @Test
    @DisplayName("Una actualización no puede incluir campos controlados por el servidor")
    void actualizacionRechazaCamposDelServidor() throws Exception {
        ClienteUpdateRequest request = mapper.readValue(
                "{\"id\":1,\"curp\":\"HELC950815HDFRPR03\",\"activo\":false,\"numeroCuenta\":\"4152000000000001\"}",
                ClienteUpdateRequest.class);

        String mensaje = errores(request).get("camposNoPermitidos");

        assertNotNull(mensaje);
        assertTrue(mensaje.contains("curp, activo, numeroCuenta"), mensaje);
    }

    @Test
    @DisplayName("El mensaje de campos no permitidos muestra el nombre recibido tal cual, sin interpretarlo")
    void camposNoPermitidosSinInterpolacion() throws Exception {
        ClienteIdRequest request = mapper.readValue("{\"id\":1,\"${1+1}\":\"x\"}", ClienteIdRequest.class);

        assertTrue(errores(request).get("camposNoPermitidos").contains("${1+1}"));
    }

    @Test
    @DisplayName("La actualización exige el id del cliente en el cuerpo")
    void actualizacionExigeId() {
        assertEquals("El id del cliente es obligatorio", errores(ClienteUpdateRequest.builder().build()).get("id"));
        assertTrue(errores(ClienteIdRequest.builder().id(0L).build()).get("id").contains("mayor a 0"));
    }

    @Test
    @DisplayName("La búsqueda valida formato de cada filtro y el rango de fechas")
    void busquedaValidaFiltros() {
        ClienteBusquedaRequest request = ClienteBusquedaRequest.builder()
                .id(-1L)
                .curp("XYZ")
                .rfc("123")
                .correo("no-es-correo")
                .numeroCuenta("12ab")
                .nombre("x1")
                .fechaRegistroDesde(LocalDate.of(2026, 6, 30))
                .fechaRegistroHasta(LocalDate.of(2026, 1, 1))
                .pagina(-1)
                .tamanio(500)
                .build();

        Map<String, String> errores = errores(request);

        assertEquals(java.util.Set.of("id", "curp", "rfc", "correo", "numeroCuenta", "nombre",
                "rangoFechaRegistroValido", "pagina", "tamanio"), errores.keySet(), errores.toString());
        assertEquals("La fecha de registro inicial no puede ser posterior a la fecha final", errores.get("rangoFechaRegistroValido"));
    }

    @Test
    @DisplayName("Una búsqueda sin filtros es válida y usa la paginación por defecto")
    void busquedaSinFiltros() throws Exception {
        ClienteBusquedaRequest request = mapper.readValue("{}", ClienteBusquedaRequest.class);

        assertTrue(errores(request).isEmpty());
        assertEquals(0, request.getPagina());
        assertEquals(20, request.getTamanio());
    }

    @Test
    @DisplayName("La búsqueda de cuentas solo acepta estatus del catálogo")
    void busquedaCuentasEstatus() {
        assertEquals("El estatus debe ser ACTIVA o CANCELADA",
                errores(CuentaBusquedaRequest.builder().estatus("BORRADA").build()).get("estatus"));
    }

    @Test
    @DisplayName("Layaway exige bandera válida y clienteId para actualizar o eliminar")
    void layawayBanderaYClienteId() {
        assertTrue(errores(LayawayClienteRequest.builder().build()).get("bandera").contains("obligatoria"));
        assertTrue(errores(LayawayClienteRequest.builder().bandera(7).build()).get("bandera").contains("1 (Insertar)"));
        assertNotNull(errores(LayawayClienteRequest.builder().bandera(2).build()).get("clienteIdPresenteSiSeRequiere"));
        assertTrue(errores(LayawayClienteRequest.builder().bandera(3).clienteId(5L).build()).isEmpty());
    }

    @Test
    @DisplayName("El login valida bandera y Face ID")
    void loginValidaciones() {
        Map<String, String> errores = errores(LoginRequest.builder().username("ana").bandera(9).faceIdBiometrico(-1L).build());

        assertTrue(errores.containsKey("bandera"));
        assertTrue(errores.containsKey("faceIdBiometrico"));
    }

    @Test
    @DisplayName("Transacción: valida cuenta, ids, monto, longitudes de teléfono/referencia y el 2FA (alias \"2fa\")")
    void transaccionValidaciones() throws Exception {
        com.proyecto.servicios.model.transaccion.request.TransaccionRequest valida = mapper.readValue(
                "{\"numCuenta\":\"4152000000000001\",\"idServicio\":133,\"idProducto\":582,\"monto\":10,"
                        + "\"telefono\":\"5555555551\",\"2fa\":\"123456\"}",
                com.proyecto.servicios.model.transaccion.request.TransaccionRequest.class);
        assertTrue(errores(valida).isEmpty(), errores(valida).toString());
        assertEquals("123456", valida.getCodigo2fa());

        Map<String, String> errores = errores(com.proyecto.servicios.model.transaccion.request.TransaccionRequest.builder()
                .numCuenta("123").idServicio(0).idProducto(-1L).monto(new java.math.BigDecimal("10.555"))
                .telefono("55-55").referencia("ref con espacios").codigo2fa("12a").build());
        assertEquals(java.util.Set.of("numCuenta", "idServicio", "idProducto", "monto", "telefono", "referencia", "codigo2fa"),
                errores.keySet(), errores.toString());
    }

    @Test
    @DisplayName("Transacción: exige teléfono (recarga) o referencia (pago de servicio)")
    void transaccionTelefonoOReferencia() {
        Map<String, String> errores = errores(com.proyecto.servicios.model.transaccion.request.TransaccionRequest.builder()
                .numCuenta("4152000000000001").idServicio(76).idProducto(205L).monto(java.math.BigDecimal.TEN)
                .codigo2fa("123456").build());
        assertTrue(errores.get("telefonoOReferencia").contains("luz o agua"), errores.toString());
    }
}
