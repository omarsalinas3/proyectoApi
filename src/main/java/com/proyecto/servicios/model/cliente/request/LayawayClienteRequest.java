package com.proyecto.servicios.model.cliente.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.cliente.dto.DatosContactoDTO;
import com.proyecto.servicios.model.cliente.dto.DatosPersonalesDTO;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
public class LayawayClienteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Bandera de operación: 1 = Insertar, 2 = Actualizar, 3 = Eliminar/Baja Lógica", example = "0")
    private Integer bandera;

    @Schema(description = "ID del cliente (requerido para bandera 2 y 3)", example = "0")
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
}
