package com.proyecto.servicios.controller;

import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifica las restricciones del proyecto sobre todos los controladores REST de la aplicación.
 */
@DisplayName("Reglas de arquitectura - Controladores REST")
class ReglasControladoresTest {

    private static List<Class<?>> controladores() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<Class<?>> clases = new ArrayList<>();
        for (var bean : scanner.findCandidateComponents("com.proyecto.servicios.controller")) {
            clases.add(Class.forName(bean.getBeanClassName()));
        }
        return clases;
    }

    private static List<Method> endpoints(Class<?> controlador) {
        return Arrays.stream(controlador.getDeclaredMethods())
                .filter(m -> Arrays.stream(m.getAnnotations())
                        .anyMatch(a -> a.annotationType().isAnnotationPresent(RequestMapping.class)
                                || a.annotationType() == RequestMapping.class))
                .toList();
    }

    @Test
    @DisplayName("Ningún endpoint usa @PathVariable ni @RequestParam")
    void sinPathVariableNiRequestParam() throws Exception {
        List<String> infracciones = new ArrayList<>();
        for (Class<?> controlador : controladores()) {
            for (Method metodo : endpoints(controlador)) {
                for (Parameter parametro : metodo.getParameters()) {
                    if (parametro.isAnnotationPresent(PathVariable.class) || parametro.isAnnotationPresent(RequestParam.class)) {
                        infracciones.add(controlador.getSimpleName() + "." + metodo.getName() + "(" + parametro.getName() + ")");
                    }
                }
            }
        }
        assertTrue(infracciones.isEmpty(), "Endpoints con @PathVariable/@RequestParam: " + infracciones);
    }

    @Test
    @DisplayName("Todo @RequestBody de clientes, cuentas, autenticación y transacciones se valida con @Valid")
    void requestBodyConValid() throws Exception {
        List<String> infracciones = new ArrayList<>();
        for (Class<?> controlador : List.of(ClienteController.class, CuentaController.class, AuthController.class,
                LayawayClienteController.class, TransaccionController.class)) {
            for (Method metodo : endpoints(controlador)) {
                for (Parameter parametro : metodo.getParameters()) {
                    if (parametro.isAnnotationPresent(RequestBody.class) && !parametro.isAnnotationPresent(Valid.class)) {
                        infracciones.add(controlador.getSimpleName() + "." + metodo.getName());
                    }
                }
            }
        }
        assertTrue(infracciones.isEmpty(), "@RequestBody sin @Valid: " + infracciones);
    }

    @Test
    @DisplayName("Ningún endpoint recibe ni devuelve entidades JPA")
    void sinEntidadesJpaEnLaFirma() throws Exception {
        List<String> infracciones = new ArrayList<>();
        for (Class<?> controlador : controladores()) {
            for (Method metodo : endpoints(controlador)) {
                if (contieneEntidad(metodo.getGenericReturnType())) {
                    infracciones.add(controlador.getSimpleName() + "." + metodo.getName() + " (retorno)");
                }
                for (Type tipo : metodo.getGenericParameterTypes()) {
                    if (contieneEntidad(tipo)) {
                        infracciones.add(controlador.getSimpleName() + "." + metodo.getName() + " (parámetro)");
                    }
                }
            }
        }
        assertTrue(infracciones.isEmpty(), "Entidades JPA expuestas: " + infracciones);
    }

    private static boolean contieneEntidad(Type tipo) {
        if (tipo instanceof Class<?> clase) {
            return clase.isAnnotationPresent(Entity.class);
        }
        if (tipo instanceof ParameterizedType parametrizado) {
            return contieneEntidad(parametrizado.getRawType())
                    || Arrays.stream(parametrizado.getActualTypeArguments()).anyMatch(ReglasControladoresTest::contieneEntidad);
        }
        return false;
    }
}
