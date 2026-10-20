package com.proyecto.servicios.model.common;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.proyecto.servicios.validation.SinCamposNoPermitidos;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base de los Request DTO que no aceptan campos fuera de su contrato.
 *
 * <p>La configuración global de Jackson ignora propiedades desconocidas; esta clase las captura para que
 * {@link SinCamposNoPermitidos} las rechace con un 400. Así el cliente no puede intentar modificar campos
 * controlados por el servidor (ej. {@code activo}, {@code curp} en una actualización, {@code numeroCuenta},
 * {@code saldo}, {@code fechaRegistro}).</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public abstract class RequestEstricto {

    @JsonIgnore
    @SinCamposNoPermitidos
    private final transient Map<String, Object> camposNoPermitidos = new LinkedHashMap<>();

    @JsonAnySetter
    public void registrarCampoNoPermitido(String nombre, Object valor) {
        camposNoPermitidos.put(nombre, valor);
    }

    @JsonIgnore
    public Map<String, Object> getCamposNoPermitidos() {
        return camposNoPermitidos;
    }
}
