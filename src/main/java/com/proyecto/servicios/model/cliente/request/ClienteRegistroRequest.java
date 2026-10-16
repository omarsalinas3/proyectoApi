package com.proyecto.servicios.model.cliente.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.proyecto.servicios.model.cliente.dto.DomicilioDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request DTO para el registro integral de Clientes Personas Físicas (Onboarding).
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Petición de registro de cliente persona física con validaciones de negocio")
public class ClienteRegistroRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    // ─────────────────────────────
    // DATOS PERSONALES
    // ─────────────────────────────

    @Schema(description = "Primer nombre del cliente", example = "Carlos")
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    private String nombre;

    @Schema(description = "Segundo nombre (opcional)", example = "Eduardo")
    @Size(max = 50, message = "El segundo nombre no debe exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    private String segundoNombre;

    @Schema(description = "Apellido paterno", example = "Hernández")
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido paterno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    private String apellidoPaterno;

    @Schema(description = "Apellido materno", example = "López")
    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido materno debe tener entre 2 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]+$", message = "El apellido materno solo debe contener letras y espacios")
    private String apellidoMaterno;

    @Schema(description = "Fecha de nacimiento (debe ser mayor de 18 años)", example = "1995-08-15")
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaNacimiento;

    @Schema(description = "Clave Única de Registro de Población (18 caracteres)", example = "HELC950815HDFRPR03")
    @NotBlank(message = "La CURP es obligatoria")
    @Size(min = 18, max = 18, message = "La CURP debe contener exactamente 18 caracteres")
    @Pattern(
            regexp = "^[A-Z]{1}[AEIOU]{1}[A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HM]{1}(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z]{1}[0-9]{1}$",
            message = "El formato de la CURP no es válido según el estándar oficial mexicano"
    )
    private String curp;

    @Schema(description = "Registro Federal de Contribuyentes (12 o 13 caracteres)", example = "HELC950815ABC")
    @NotBlank(message = "El RFC es obligatorio")
    @Size(min = 12, max = 13, message = "El RFC debe contener 12 o 13 caracteres")
    @Pattern(
            regexp = "^[A-Z&Ññ]{3,4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{3}$",
            message = "El formato del RFC no es válido según el estándar oficial del SAT"
    )
    private String rfc;

    @Schema(description = "Sexo (M = Masculino, F = Femenino)", example = "M")
    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = "(?i)^(M|F|H|MASCULINO|FEMENINO|HOMBRE|MUJER|OTRO)$",
            message = "El sexo no es válido. Valores permitidos: M, F, MASCULINO, FEMENINO, OTRO")
    private String sexo;

    @Schema(description = "Nacionalidad", example = "Mexicana")
    @NotBlank(message = "La nacionalidad es obligatoria")
    @Size(min = 4, max = 50, message = "La nacionalidad debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+( [a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+)*$",
            message = "La nacionalidad solo debe contener letras y espacios simples (sin números ni símbolos), por ejemplo: Mexicana")
    private String nacionalidad;

    @Schema(description = "Estado civil (SOLTERO, CASADO, UNION_LIBRE, etc.)", example = "SOLTERO")
    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "(?i)^(SOLTERO|SOLTERA|CASADO|CASADA|UNION[ _]?LIBRE|UNIÓN[ _]?LIBRE|CONCUBINATO|DIVORCIADO|DIVORCIADA|VIUDO|VIUDA)$",
            message = "El estado civil no es válido. Valores permitidos: SOLTERO, CASADO, UNION_LIBRE, DIVORCIADO, VIUDO")
    private String estadoCivil;

    // ─────────────────────────────
    // DATOS DE CONTACTO
    // ─────────────────────────────

    @Schema(description = "Correo electrónico institucional o personal", example = "carlos.hernandez@example.com")
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$",
            message = "El correo electrónico debe tener el formato usuario@dominio.com (con dominio y extensión válidos)")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correo;

    @Schema(description = "Teléfono móvil a 10 dígitos", example = "5512345678")
    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos numéricos")
    private String telefonoMovil;

    @Schema(description = "Teléfono alternativo u oficina (opcional)", example = "5587654321")
    @Pattern(regexp = "^(\\d{10})?$", message = "El teléfono alternativo debe contener exactamente 10 dígitos numéricos")
    private String telefonoAlternativo;

    // ─────────────────────────────
    // INFORMACIÓN LABORAL
    // ─────────────────────────────

    @Schema(description = "Ocupación o profesión", example = "Ingeniero de Software")
    @NotBlank(message = "La ocupación es obligatoria")
    @Size(min = 2, max = 100, message = "La ocupación debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ]+[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ .,-]*$",
            message = "La ocupación solo debe contener letras, espacios, puntos, comas y guiones (sin números ni otros símbolos)")
    private String ocupacion;

    @Schema(description = "Empresa donde labora", example = "Tech Solutions S.A. de C.V.")
    @NotBlank(message = "La empresa es obligatoria")
    @Size(min = 2, max = 100, message = "La empresa debe tener entre 2 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9][a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9 .,&'-]*$",
            message = "La empresa solo debe contener letras, números, espacios y los símbolos . , & ' -")
    private String empresa;

    @Schema(description = "Ingreso mensual comprobable (mayor a cero)", example = "35000.00")
    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    @Schema(description = "URL o identificador del documento de identidad INE digitalizado", example = "https://storage.banco.com/docs/ine_carlos.pdf")
    @Size(max = 500, message = "La URL del documento INE no debe exceder 500 caracteres")
    @Pattern(regexp = "^(https?://\\S+)?$", message = "El documento INE debe ser una URL válida que inicie con http:// o https://")
    private String ineDocumentoUrl;

    // ─────────────────────────────
    // DOMICILIO ASOCIADO
    // ─────────────────────────────

    @Schema(description = "Domicilio particular del cliente")
    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioDTO domicilio;

    // ─────────────────────────────
    // CREDENCIALES DE ACCESO Y BIOMETRÍA
    // ─────────────────────────────

    @Schema(description = "Nombre de usuario para el inicio de sesión", example = "carlos.h95")
    @NotBlank(message = "El nombre de usuario para login es obligatorio")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
            message = "El nombre de usuario solo debe contener letras, números, punto, guion y guion bajo (sin espacios)")
    private String username;

    @Schema(description = "Contraseña en texto plano para su posterior cifrado seguro", example = "PasswordSeguro123!")
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$",
            message = "La contraseña debe incluir al menos una mayúscula, una minúscula, un número y un símbolo, sin espacios")
    private String password;

    @Schema(description = "Valor biométrico escalar o de resumen", example = "0.9876543210123456789")
    private BigDecimal datosBiometricos;

    @Schema(
            description = "Vector de características faciales extraído por MediaPipe Face Embedder (ej. 128 o 512 dimensiones en punto flotante/BigDecimal)",
            example = "[0.12345, -0.98765, 0.45678, 0.33445, -0.11223]"
    )
    private java.util.List<BigDecimal> vectorEmbeddingFacial;
}
