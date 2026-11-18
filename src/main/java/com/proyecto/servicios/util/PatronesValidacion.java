package com.proyecto.servicios.util;

import com.proyecto.servicios.model.cliente.response.ClienteResponse;

/**
 * Expresiones regulares y mensajes compartidos para validar identificadores recibidos en la URL
 * (ID, CURP, RFC y número de cuenta) antes de ejecutar la lógica de negocio.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public final class PatronesValidacion {

    public static final String MENSAJE_ID = "El id del cliente debe ser un número entero mayor a 0";

    public static final String CURP =
            "(?i)^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HM]"
                    + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
                    + "[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$";
    public static final String MENSAJE_CURP =
            "La CURP debe contener 18 caracteres con el formato oficial (ej. HELC950815HDFRPR03)";

    public static final String RFC = "(?i)^[A-Z&Ñ]{3,4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{3}$";
    public static final String MENSAJE_RFC =
            "El RFC debe contener 12 o 13 caracteres con el formato oficial del SAT (ej. HELC950815ABC)";

    public static final String NUMERO_CUENTA = "^[0-9]{16}$";
    public static final String MENSAJE_NUMERO_CUENTA = "El número de cuenta debe contener exactamente 16 dígitos numéricos";

    private static final String LETRAS = "a-zA-ZáéíóúÁÉÍÓÚñÑüÜ";

    // Cuantificadores posesivos (++ y *+): evitan el retroceso que con textos largos congelaba la validación o
    // provocaba StackOverflowError. @Size no protege, porque el validador evalúa todas las reglas del campo.
    public static final String NACIONALIDAD = "^[" + LETRAS + "]++(?: [" + LETRAS + "]++)*+$";
    public static final String OCUPACION = "^[" + LETRAS + "][" + LETRAS + " .,-]*+$";
    public static final String CORREO = "^[A-Za-z0-9._%+-]++@(?:[A-Za-z0-9-]++\\.)++[A-Za-z]{2,}+$";

    // (?u) hace que ignorar mayúsculas también aplique a letras acentuadas: sin él se rechazaba "unión libre"
    public static final String ESTADO_CIVIL = "(?iu)^(SOLTERO|SOLTERA|CASADO|CASADA|UNI[OÓ]N[ _]?LIBRE|CONCUBINATO"
            + "|DIVORCIADO|DIVORCIADA|VIUDO|VIUDA)$";

    private PatronesValidacion() {
    }

    /**
     * Indica si el número de cuenta pertenece al cliente indicado.
     */
    public static boolean esCuentaDe(ClienteResponse cliente, String numeroCuenta) {
        return cliente != null && cliente.getCuenta() != null
                && numeroCuenta != null && numeroCuenta.equals(cliente.getCuenta().getNumeroCuenta());
    }
}
