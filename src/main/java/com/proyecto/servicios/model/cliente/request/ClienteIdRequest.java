package com.proyecto.servicios.model.cliente.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import com.proyecto.servicios.util.PatronesValidacion;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Identifica a un cliente en el cuerpo de la petición (ej. baja lógica).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Identificador del cliente sobre el que se opera")
public class ClienteIdRequest extends RequestEstricto {

    @Schema(description = "Identificador del cliente", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = PatronesValidacion.MENSAJE_ID)
    private Long id;
}
