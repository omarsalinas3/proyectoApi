package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.cliente.dto.DatosContactoDTO;
import com.proyecto.servicios.model.cliente.dto.DatosPersonalesDTO;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Request DTO unificado para la gestión integral de clientes mediante banderas de operación.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Petición unificada para gestión de clientes mediante banderas de operación")
@EqualsAndHashCode(callSuper = false)
public class LayawayClienteRequest extends RequestEstricto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Bandera de operación: 1 = Insertar, 2 = Actualizar, 3 = Eliminar/Baja Lógica", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La bandera de operación es obligatoria (1 = Insertar, 2 = Actualizar, 3 = Eliminar)")
    @Min(value = 1, message = "La bandera debe ser 1 (Insertar), 2 (Actualizar) o 3 (Eliminar)")
    @Max(value = 3, message = "La bandera debe ser 1 (Insertar), 2 (Actualizar) o 3 (Eliminar)")
    private Integer bandera;

    @Schema(description = "ID del cliente (requerido para bandera 2 y 3)", example = "1")
    @Positive(message = "El clienteId debe ser un número entero mayor a 0")
    private Long clienteId;

    @Schema(description = "Datos personales del cliente")
    private DatosPersonalesDTO datosPersonales;

    @Schema(description = "Datos de contacto del cliente")
    private DatosContactoDTO datosContacto;

    @Schema(description = "Domicilio del cliente")
    private DomicilioDTO domicilio;

    @Schema(description = "Información financiera del cliente")
    private com.proyecto.servicios.model.cliente.dto.InformacionFinancieraDTO informacionFinanciera;

    @Schema(description = "Datos de autenticación inicial (contraseña y Face ID)")
    private com.proyecto.servicios.model.cliente.dto.AutenticacionDTO autenticacion;

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "El clienteId es obligatorio para las banderas 2 (Actualizar) y 3 (Eliminar)")
    public boolean isClienteIdPresenteSiSeRequiere() {
        return bandera == null || bandera == 1 || clienteId != null;
    }
}
