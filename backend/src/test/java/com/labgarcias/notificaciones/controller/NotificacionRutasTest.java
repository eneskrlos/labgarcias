package com.labgarcias.notificaciones.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Guarda estructural de §6.4 y §6.5, no test de comportamiento.
 *
 * Protege dos cosas que ningún test funcional detectaría si se rompieran: que todo endpoint
 * declare su autorización (RN-14), y sobre todo que **ninguno acepte un id de usuario**. El
 * criterio 3 de §6 se sostiene porque el destinatario sale del token; el día que alguien agregue
 * un `?usuarioId=` por comodidad, el criterio se cae en silencio y este test lo frena.
 */
class NotificacionRutasTest {

    private static final List<Class<?>> CONTROLLERS = List.of(NotificacionController.class,
            ConfiguracionNotificacionController.class, TelegramVinculacionController.class);

    private static List<Method> endpointsDe(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(metodo -> Arrays.stream(metodo.getAnnotations())
                        .anyMatch(a -> a.annotationType().getName().startsWith("org.springframework.web.bind.annotation")))
                .toList();
    }

    /** RN-14: ningún endpoint queda sin anotación de autorización. */
    @Test
    void rn14TodosLosEndpointsDeclaranSuAutorizacion() {
        List<String> sinAutorizacion = CONTROLLERS.stream()
                .flatMap(controller -> endpointsDe(controller).stream())
                .filter(metodo -> !metodo.isAnnotationPresent(PreAuthorize.class))
                .map(Method::getName)
                .toList();

        assertThat(sinAutorizacion).isEmpty();
    }

    /**
     * §6 criterio 3. El único @PathVariable de todo el módulo es el id de la notificación en
     * `/{id}/leer`, y ese va acompañado del destinatario del token en la consulta. Vale igual
     * para §6.5: vincular el Telegram de otro sería pasar a recibir sus notificaciones.
     */
    @Test
    void criterio3NingunEndpointRecibeUnIdDeUsuario() {
        List<String> parametrosDeRutaOConsulta = CONTROLLERS.stream()
                .flatMap(controller -> endpointsDe(controller).stream())
                .flatMap(metodo -> Arrays.stream(metodo.getParameters()))
                .filter(parametro -> parametro.isAnnotationPresent(PathVariable.class)
                        || parametro.isAnnotationPresent(RequestParam.class))
                .map(parametro -> parametro.getType().getSimpleName() + " " + parametro.getName())
                .toList();

        // Se afirma la lista completa, no solo la ausencia de "usuario": así, cualquier parámetro
        // nuevo obliga a pasar por acá y a justificar que no abre una puerta al dato de otro.
        assertThat(parametrosDeRutaOConsulta)
                .containsExactlyInAnyOrder("Long id", "Boolean leidas");
    }

    /** El destinatario tiene que llegar por el token: todo endpoint recibe el Authentication. */
    @Test
    void criterio3TodosLosEndpointsTomanElUsuarioDelToken() {
        List<String> sinAuthentication = CONTROLLERS.stream()
                .flatMap(controller -> endpointsDe(controller).stream())
                .filter(metodo -> Arrays.stream(metodo.getParameterTypes())
                        .noneMatch(Authentication.class::equals))
                .map(Method::getName)
                .toList();

        assertThat(sinAuthentication).isEmpty();
    }
}
