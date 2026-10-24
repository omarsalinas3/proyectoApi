package com.proyecto.servicios.model.cliente.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado del segundo factor del cliente y, solo al iniciar la activación, los datos para darlo de alta.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Estado y datos de alta del segundo factor (TOTP)")
public class DosFactoresResponse {

    @Schema(description = "true cuando el 2FA ya está confirmado y se exige en las transacciones", example = "false")
    private Boolean activo;

    @Schema(description = "Secreto Base32 para capturarlo manualmente en la app (solo se muestra al iniciar la activación)",
            example = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP")
    private String secreto;

    @Schema(description = "URI otpauth:// para generar el código QR (solo se muestra al iniciar la activación)",
            example = "otpauth://totp/ProyectoApi:ana%40example.com?secret=JBSWY3DPEHPK3PXP&issuer=ProyectoApi&algorithm=SHA1&digits=6&period=30")
    private String uriProvisionamiento;
}
