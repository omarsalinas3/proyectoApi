package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.LoginRequest;
import com.proyecto.servicios.model.cliente.response.LoginResponse;

/**
 * Interfaz de servicio para la gestión de autenticación, control de sesiones dinámicas
 * y cierre de sesión de clientes.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface AuthService {

    /**
     * Autentica a un usuario verificando credenciales cifradas y activa su sesión.
     *
     * @param request datos de inicio de sesión con usuario, contraseña y timeout dinámico opcional.
     * @return respuesta con el estado de la sesión activa y metadatos.
     */
    LoginResponse login(LoginRequest request);

    /**
     * Valida el estado de la sesión activa y evalúa si ha superado el tiempo de inactividad permitido.
     * Si supera el límite, la sesión pasa automáticamente a inactiva (false).
     *
     * @param username identificador del usuario.
     * @param tiempoInactividadSegundos timeout dinámico en segundos para esta consulta (opcional).
     * @return estado actual de la sesión si sigue activa.
     */
    LoginResponse verificarEstadoSesion(String username, Integer tiempoInactividadSegundos);

    /**
     * Cierra formalmente la sesión de un usuario, desactivando su bandera de sesión activa.
     *
     * @param username identificador del usuario.
     * @return estado de confirmación de cierre de sesión.
     */
    LoginResponse logout(String username);
}
