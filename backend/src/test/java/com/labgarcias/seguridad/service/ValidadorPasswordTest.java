package com.labgarcias.seguridad.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.labgarcias.shared.excepcion.DominioException;
import com.labgarcias.shared.excepcion.ValidacionException;

class ValidadorPasswordTest {

    /** RN-15: mínimo 9, con mayúsculas, minúsculas, números y especiales. */
    @Test
    void aceptaUnaContrasenaQueCumpleRn15() {
        assertThat(ValidadorPassword.cumpleRn15("Abcdef1$x")).isTrue();
    }

    @ParameterizedTest(name = "rechaza \"{0}\"")
    @ValueSource(strings = {
            "Abc1$xy",        // menos de 9
            "abcdef1$xyz",    // sin mayúscula
            "ABCDEF1$XYZ",    // sin minúscula
            "Abcdefgh$x",     // sin dígito
            "Abcdefg1xy"      // sin especial
    })
    void rechazaLoQueNoCumpleRn15(String password) {
        assertThat(ValidadorPassword.cumpleRn15(password)).isFalse();
    }

    @Test
    void unaContrasenaNulaNoCumple() {
        assertThat(ValidadorPassword.cumpleRn15(null)).isFalse();
    }

    @Test
    void validarLanzaConCodigoYCampoParaElFrontend() {
        assertThatThrownBy(() -> ValidadorPassword.validar("corta", "passwordNueva"))
                .isInstanceOf(ValidacionException.class)
                .extracting(excepcion -> ((DominioException) excepcion).getCodigo())
                .isEqualTo("PASSWORD_INVALIDA");
    }

    /** El mensaje de error nunca puede repetir la contraseña que mandó el usuario. */
    @Test
    void elMensajeDeErrorNoIncluyeLaContrasenaRechazada() {
        assertThatThrownBy(() -> ValidadorPassword.validar("secreto123", "passwordNueva"))
                .hasMessageNotContaining("secreto123");
    }
}
