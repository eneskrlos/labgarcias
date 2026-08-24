package com.labgarcias.ordenes.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Guardas estructurales de los dos paneles (§5.7), con el mismo criterio que
 * `NotificacionRutasTest`: no prueban comportamiento, impiden que una tarea futura afloje una
 * regla sin darse cuenta.
 */
class DashboardRutasTest {

    private static final List<Class<?>> CONTROLLERS =
            List.of(DashboardController.class, AdminDashboardController.class);

    private static List<Method> endpoints() {
        return CONTROLLERS.stream()
                .flatMap(controller -> Arrays.stream(controller.getDeclaredMethods()))
                .filter(metodo -> Arrays.stream(metodo.getAnnotations())
                        .anyMatch(a -> a.annotationType().getName()
                                .startsWith("org.springframework.web.bind.annotation")))
                .toList();
    }

    /** RN-14: ningún endpoint de panel queda sin anotación de autorización. */
    @Test
    void rn14TodosLosEndpointsDeclaranSuAutorizacion() {
        assertThat(endpoints())
                .allSatisfy(metodo -> assertThat(metodo.isAnnotationPresent(PreAuthorize.class))
                        .as("%s sin @PreAuthorize", metodo.getName())
                        .isTrue());
    }

    /** Los paneles son de lectura: nada de lo que muestran se cambia desde acá. */
    @Test
    void losPanelesSonSoloDeLectura() {
        List<Class<? extends java.lang.annotation.Annotation>> escrituras =
                List.of(PostMapping.class, PutMapping.class, PatchMapping.class, DeleteMapping.class);

        assertThat(endpoints())
                .allSatisfy(metodo -> assertThat(escrituras)
                        .as("%s no debería escribir: un panel solo consulta", metodo.getName())
                        .noneMatch(metodo::isAnnotationPresent));
    }

    /**
     * RN-01: el panel del odontólogo no acepta **ningún** parámetro de consulta. El dueño sale del
     * token; en cuanto exista un `?odontologoId=`, un odontólogo podría ver el panel de otro.
     */
    @Test
    void rn01ElPanelDelOdontologoNoAceptaNingunParametroDeConsulta() {
        List<String> parametros = Arrays.stream(DashboardController.class.getDeclaredMethods())
                .flatMap(metodo -> Arrays.stream(metodo.getParameters()))
                .filter(parametro -> parametro.isAnnotationPresent(RequestParam.class))
                .map(Parameter::getName)
                .toList();

        assertThat(parametros).isEmpty();
    }
}
