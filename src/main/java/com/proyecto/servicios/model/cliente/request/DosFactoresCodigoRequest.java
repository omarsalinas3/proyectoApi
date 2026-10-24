package com.proyecto.servicios.model.cliente.request;

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
 * Código de 6 dígitos generado por la app de autenticación, para confirmar la activación del 2FA.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@Schema(description = "Código TOTP de 6 dígitos de la app de autenticación")
public class DosFactoresCodigoRequest extends RequestEstricto {

    @Schema(description = "Código vigente de la app de autenticación", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El código de verificación es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código de verificación debe contener exactamente 6 dígitos")
    private String codigo;
}
