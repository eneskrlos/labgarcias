package com.labgarcias.ordenes.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;

/**
 * §5.6 criterio 1 / RN-17: la orden es inmutable una vez creada. El único endpoint que
 * el odontólogo tiene sobre una orden existente es la cancelación.
 *
 * Es una guarda estructural, no un test de comportamiento: protege contra que alguien
 * agregue más adelante un PUT /ordenes/{id} sin advertir que RN-17 lo prohíbe.
 *
 * Cubre los dos controllers del recurso: el del odontólogo y el del laboratorio (§5.7). RN-17
 * vale para los dos —nadie edita una orden— y la cancelación sigue siendo solo del propietario.
 */
class OrdenControllerRutasTest {

    private static final List<Class<?>> CONTROLLERS =
            List.of(OrdenController.class, AdminOrdenController.class);

    private static List<Method> metodosCon(Class<? extends Annotation> anotacion) {
        return CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(metodo -> metodo.isAnnotationPresent(anotacion))
                .toList();
    }

    @Test
    void criterio1NoExisteNingunEndpointDeEdicionDeOrden() {
        assertThat(metodosCon(PutMapping.class))
                .as("RN-17: no debe existir PUT /ordenes/{id}")
                .isEmpty();
        assertThat(metodosCon(DeleteMapping.class))
                .as("RN-17: una orden no se borra, se cancela")
                .isEmpty();
    }

    @Test
    void losUnicosPatchSonAvanzarEstadoYCancelar() {
        List<String> rutas = metodosCon(PatchMapping.class).stream()
                .flatMap(metodo -> Arrays.stream(metodo.getAnnotation(PatchMapping.class).value()))
                .toList();

        assertThat(rutas).containsExactlyInAnyOrder("/{id}/estado", "/{id}/cancelar");
    }

    /** RN-14: ningún endpoint del controller queda sin anotación de autorización. */
    @Test
    void rn14TodosLosEndpointsDeclaranSuAutorizacion() {
        List<String> sinAutorizacion = CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(metodo -> Arrays.stream(metodo.getAnnotations())
                        .anyMatch(a -> a.annotationType().getName().startsWith("org.springframework.web.bind.annotation")))
                .filter(metodo -> !metodo.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .toList();

        assertThat(sinAutorizacion).isEmpty();
    }
}
