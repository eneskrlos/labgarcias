package com.labgarcias.shared.excepcion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

/** Protege el mapeo codigo->HttpStatus de cada rama de la jerarquía (spec.md §1). */
class DominioExceptionHttpStatusTest {

    @Test
    void recursoNoEncontradoEs404() {
        assertThat(new RecursoNoEncontradoException("X", "m").getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void conflictoEs409() {
        assertThat(new ConflictoException("X", "m").getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void reglaNegocioEs422() {
        assertThat(new ReglaNegocioException("X", "m").getHttpStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void validacionEs400() {
        assertThat(new ValidacionException("X", "m", "campo").getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void noAutenticadoEs401() {
        assertThat(new NoAutenticadoException("X", "m").getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void accesoDenegadoEs403() {
        assertThat(new AccesoDenegadoException("X", "m").getHttpStatus()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void elCodigoYElCampoQuedanAccesibles() {
        DominioException excepcion = new ConflictoException("CORREO_YA_REGISTRADO", "mensaje", "correo");

        assertThat(excepcion.getCodigo()).isEqualTo("CORREO_YA_REGISTRADO");
        assertThat(excepcion.getCampo()).isEqualTo("correo");
        assertThat(excepcion.getMessage()).isEqualTo("mensaje");
    }
}
