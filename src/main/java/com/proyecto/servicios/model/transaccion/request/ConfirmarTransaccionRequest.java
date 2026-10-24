package com.proyecto.servicios.model.transaccion.request;

import com.proyecto.servicios.model.common.RequestEstricto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Transacción pendiente cuyo resultado se consulta con {@code confirmTx}.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "UPC de la transacción pendiente de confirmación")
public class ConfirmarTransaccionRequest extends RequestEstricto {

    @Schema(description = "UPC devuelto al enviar la transacción", example = "TX4K9Z2M7Q1A8B3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El UPC es obligatorio")
    @Pattern(regexp = "^[A-Za-z0-9_]{3,15}$", message = "El UPC debe tener entre 3 y 15 caracteres alfanuméricos")
    private String upc;
}
