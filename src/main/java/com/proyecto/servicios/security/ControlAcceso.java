package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Predicate;

/**
 * Autorización a nivel de registro: garantiza que un cliente solo pueda consultar o modificar su propia
 * información aunque cambie el ID, CURP, RFC, correo o número de cuenta en la URL. El rol EJECUTIVO
 * puede acceder a cualquier cliente.
 *
 * <p>Las verificaciones se ejecutan antes de invocar la lógica de negocio y, para clientes, se resuelven
 * comparando contra su propio registro, de modo que un 403 no revela si el recurso ajeno existe.</p>
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ControlAcceso {

    private final ClienteService clienteService;

    /**
     * Devuelve el usuario autenticado de la petición actual.
     *
     * @throws NoAutenticadoException si la petición no trae un token válido.
     */
    public UsuarioAutenticado usuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new NoAutenticadoException(
                    "Debe iniciar sesión y enviar el token en el encabezado 'Authorization: Bearer <token>'");
        }
        return usuario;
    }

    /**
     * Permite el acceso solo al ejecutivo o al cliente cuyo ID coincide con el del token.
     */
    public void validarAccesoCliente(Long clienteId) {
        UsuarioAutenticado usuario = usuarioActual();
        if (usuario.esEjecutivo()) {
            return;
        }
        if (clienteId == null || !clienteId.equals(usuario.clienteId())) {
            log.warn("Acceso denegado: el cliente {} intentó acceder al cliente {}", usuario.clienteId(), clienteId);
            throw new AccesoDenegadoException("No tiene permisos para acceder a la información del cliente solicitado");
        }
    }

    /**
     * Permite el acceso solo al ejecutivo o al cliente cuyo propio registro cumple la condición
     * (ej. su CURP o su número de cuenta coinciden con el solicitado).
     *
     * @param perteneceAlCliente condición evaluada sobre el registro del cliente autenticado.
     * @param recurso            descripción del recurso para el mensaje de error (ej. "la cuenta").
     */
    public void validarRecursoPropio(Predicate<ClienteResponse> perteneceAlCliente, String recurso) {
        UsuarioAutenticado usuario = usuarioActual();
        if (usuario.esEjecutivo()) {
            return;
        }
        ClienteResponse propio = clienteService.consultarPorId(usuario.clienteId());
        if (!perteneceAlCliente.test(propio)) {
            log.warn("Acceso denegado: el cliente {} intentó consultar {} de otro cliente", usuario.clienteId(), recurso);
            throw new AccesoDenegadoException("No tiene permisos para consultar " + recurso + " solicitado");
        }
    }

    /**
     * Restringe la operación al rol EJECUTIVO (consultas masivas de clientes y cuentas).
     */
    public void validarRolEjecutivo() {
        UsuarioAutenticado usuario = usuarioActual();
        if (!usuario.esEjecutivo()) {
            log.warn("Acceso denegado: el cliente {} intentó una consulta reservada para ejecutivos", usuario.clienteId());
            throw new AccesoDenegadoException("Esta consulta está reservada para ejecutivos de la institución");
        }
    }
}
