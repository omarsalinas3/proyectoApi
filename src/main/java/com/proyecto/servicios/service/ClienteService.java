package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.request.ClienteRegistroRequest;
import com.proyecto.servicios.model.cliente.request.ClienteUpdateRequest;
import com.proyecto.servicios.model.cliente.response.ClienteResponse;

import java.time.LocalDate;
import java.util.List;

/**
 * Interfaz del servicio principal para el proceso de Onboarding, consulta,
 * actualización y baja lógica de Clientes Personas Físicas.
 *
 * @author Equipo de Desarrollo
 * @version 1.0
 */
public interface ClienteService {

    /**
     * Registra un nuevo cliente persona física, generando automáticamente su cuenta bancaria de 16 dígitos,
     * saldo inicial de $0.00 y credenciales cifradas de acceso.
     *
     * @param request datos completos de registro validados.
     * @return respuesta con los datos del cliente y su cuenta generada.
     */
    ClienteResponse registrarCliente(ClienteRegistroRequest request);

    /**
     * Actualiza los datos permitidos del cliente protegiendo la inmutabilidad de CURP, RFC y cuentas.
     *
     * @param id identificador único del cliente.
     * @param request datos actualizables.
     * @return respuesta con los datos actualizados.
     */
    ClienteResponse actualizarCliente(Long id, ClienteUpdateRequest request);

    /**
     * Realiza la baja lógica de un cliente y cancela sus cuentas asociadas.
     *
     * @param id identificador único del cliente.
     * @return respuesta del cliente con estatus inactivo.
     */
    ClienteResponse eliminarClienteLogico(Long id);

    /**
     * Consulta un cliente por su identificador único.
     *
     * @param id identificador del cliente.
     * @return datos completos del cliente.
     */
    ClienteResponse consultarPorId(Long id);

    /**
     * Consulta un cliente por su CURP oficial.
     *
     * @param curp clave única de registro de población.
     * @return datos completos del cliente.
     */
    ClienteResponse consultarPorCurp(String curp);

    /**
     * Consulta un cliente por su RFC.
     *
     * @param rfc registro federal de contribuyentes.
     * @return datos completos del cliente.
     */
    ClienteResponse consultarPorRfc(String rfc);

    /**
     * Consulta un cliente por su correo electrónico.
     *
     * @param correo dirección de correo electrónico.
     * @return datos completos del cliente.
     */
    ClienteResponse consultarPorCorreo(String correo);

    /**
     * Consulta un cliente a partir de su número de cuenta bancaria de 16 dígitos.
     *
     * @param numeroCuenta número de cuenta de 16 dígitos.
     * @return datos completos del cliente titular.
     */
    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);

    /**
     * Consulta todos los clientes registrados, con opción de filtrar solo activos.
     *
     * @param soloActivos si es {@code true}, solo retorna clientes con estatus activo.
     * @return lista de clientes encontrados.
     */
    List<ClienteResponse> consultarTodos(Boolean soloActivos);

    /**
     * Consulta clientes registrados dentro de un rango de fechas.
     *
     * @param inicio fecha inicial del rango.
     * @param fin fecha final del rango.
     * @return lista de clientes registrados en dicho periodo.
     */
    List<ClienteResponse> consultarPorRangoFechas(LocalDate inicio, LocalDate fin);
}
