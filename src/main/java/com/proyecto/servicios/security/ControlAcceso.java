package com.proyecto.servicios.security;

import com.proyecto.servicios.exception.AccesoDenegadoException;
import com.proyecto.servicios.exception.NoAutenticadoException;
import com.proyecto.servicios.model.cliente.request.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.request.CuentaBusquedaRequest;
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
     * Limita una búsqueda de clientes a lo que el usuario puede ver: el ejecutivo busca sin restricción y un
     * cliente solo puede obtener su propio registro (si indica el id de otro cliente se rechaza con 403).
     *
     * @return los mismos filtros, con el id del cliente autenticado forzado cuando el rol es CLIENTE.
     */
    public ClienteBusquedaRequest limitarBusquedaClientes(ClienteBusquedaRequest filtros) {
        UsuarioAutenticado usuario = usuarioActual();
        if (usuario.esEjecutivo()) {
            return filtros;
        }
        if (filtros.getId() != null && !filtros.getId().equals(usuario.clienteId())) {
            log.warn("Acceso denegado: el cliente {} intentó buscar al cliente {}", usuario.clienteId(), filtros.getId());
            throw new AccesoDenegadoException("No tiene permisos para consultar la información de otros clientes");
        }
        filtros.setId(usuario.clienteId());
        return filtros;
    }

    /**
     * Limita una búsqueda de cuentas: un cliente solo puede obtener sus propias cuentas.
     *
     * @return los mismos filtros, con el titular forzado al cliente autenticado cuando el rol es CLIENTE.
     */
    public CuentaBusquedaRequest limitarBusquedaCuentas(CuentaBusquedaRequest filtros) {
        UsuarioAutenticado usuario = usuarioActual();
        if (usuario.esEjecutivo()) {
            return filtros;
        }
        if (filtros.getClienteId() != null && !filtros.getClienteId().equals(usuario.clienteId())) {
            log.warn("Acceso denegado: el cliente {} intentó buscar cuentas del cliente {}", usuario.clienteId(), filtros.getClienteId());
            throw new AccesoDenegadoException("No tiene permisos para consultar cuentas de otros clientes");
        }
        filtros.setClienteId(usuario.clienteId());
        return filtros;
    }
}
