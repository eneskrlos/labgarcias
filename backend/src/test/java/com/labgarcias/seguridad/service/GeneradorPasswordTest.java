package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.labgarcias.shared.util.ConstantesDominio;

class GeneradorPasswordTest {

    private final GeneradorPassword generador = new GeneradorPassword();

    /**
     * §3.1.b criterio 1: la contraseña generada cumple RN-15. Se repite porque el valor es
     * aleatorio: una sola corrida no probaría que *siempre* cumple.
     */
    @RepeatedTest(50)
    void criterio1LaContrasenaGeneradaCumpleRn15() {
        assertThat(ValidadorPassword.cumpleRn15(generador.generar())).isTrue();
    }

    @Test
    void respetaLaLongitudMinimaDeRn15() {
        assertThat(generador.generar().length())
                .isGreaterThanOrEqualTo(ConstantesDominio.LONGITUD_MINIMA_PASSWORD);
    }

    /** Dos altas seguidas no pueden compartir contraseña. */
    @Test
    void generaUnaContrasenaDistintaCadaVez() {
        Set<String> generadas = new HashSet<>();
        IntStream.range(0, 200).forEach(i -> generadas.add(generador.generar()));

        assertThat(generadas).hasSize(200);
    }

    /** Sin espacios: se copia y pega desde un correo, y un espacio al borde se pierde. */
    @Test
    void noContieneEspacios() {
        assertThat(generador.generar()).doesNotContain(" ");
    }
}
