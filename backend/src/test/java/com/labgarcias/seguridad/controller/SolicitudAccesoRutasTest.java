package com.labgarcias.seguridad.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Guarda estructural de §3.1 y §3.1.b, no test de comportamiento.
 *
 * Fija dos cosas que ningún test funcional detectaría si se rompieran: que la gestión de
 * solicitudes queda reservada a las cuentas de administración (RN-14), y que el único endpoint
 * público del módulo sigue siendo el formulario de solicitud — el día que alguien marque otro
 * como `permitAll()`, este test lo frena.
 */
class SolicitudAccesoRutasTest {

    private static final String ROLES_ADMINISTRACION = "hasAnyRole('ADMIN','SUPERADMIN')";

    private static List<Method> endpointsDe(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(metodo -> Arrays.stream(metodo.getAnnotations())
                        .anyMatch(a -> a.annotationType().getName().startsWith("org.springframework.web.bind.annotation")))
                .toList();
    }

    /** RN-14: ningún endpoint queda sin anotación de autorización. */
    @Test
    void rn14TodosLosEndpointsDeclaranSuAutorizacion() {
        List<String> sinAutorizacion = Arrays
                .asList(SolicitudAccesoController.class, AuthController.class, OdontologoController.class).stream()
                .flatMap(controller -> endpointsDe(controller).stream())
                .filter(metodo -> !metodo.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .toList();

        assertThat(sinAutorizacion).isEmpty();
    }

    /** La gestión de solicitudes es de las cuentas de administración, las mismas que reciben el aviso. */
    @Test
    void laGestionDeSolicitudesEsSoloDeAdministracion() {
        List<String> autorizaciones = endpointsDe(SolicitudAccesoController.class).stream()
                .map(metodo -> metodo.getAnnotation(PreAuthorize.class).value())
                .distinct()
                .toList();

        assertThat(autorizaciones).containsExactly(ROLES_ADMINISTRACION);
    }

    /** D-18: dar de alta una cuenta es del administrador, nunca del odontólogo. */
    @Test
    void elAltaDeOdontologoEsSoloDeAdministracion() {
        List<String> autorizaciones = endpointsDe(OdontologoController.class).stream()
                .map(metodo -> metodo.getAnnotation(PreAuthorize.class).value())
                .distinct()
                .toList();

        assertThat(autorizaciones).containsExactly(ROLES_ADMINISTRACION);
    }

    /** §3.1: el formulario de solicitud es público; el resto de `auth`, no. */
    @Test
    void soloElFormularioDeSolicitudEsPublico() {
        List<String> publicos = endpointsDe(AuthController.class).stream()
                .filter(metodo -> metodo.getAnnotation(PreAuthorize.class).value().equals("permitAll()"))
                .map(Method::getName)
                .toList();

        assertThat(publicos).containsExactlyInAnyOrder("login", "solicitarAcceso");
    }
}
