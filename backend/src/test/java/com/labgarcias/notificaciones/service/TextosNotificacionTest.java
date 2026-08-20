package com.labgarcias.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextosNotificacionTest {

    /** CU-07 fija el texto palabra por palabra. Si esto falla, cambió la spec o se rompió el formato. */
    @Test
    void cu07UsaElTextoDocumentadoTalCual() {
        assertThat(TextosNotificacion.cambioEstado(1000, "En producción"))
                .isEqualTo("El trabajo del paciente Código 1000 pasó a la etapa de En producción.");
    }

    /**
     * RN-03/RN-22: el mensaje se persiste y además sale por correo. Ningún texto puede
     * identificar al paciente por su nombre: solo por el código.
     */
    @Test
    void rn22NingunTextoNombraAlPaciente() {
        assertThat(TextosNotificacion.cambioEstado(1000, "Listo")).doesNotContain("Martín");
        assertThat(TextosNotificacion.nuevaOrden("LG-0001", 1000)).contains("1000").doesNotContain("Martín");
        assertThat(TextosNotificacion.ordenUrgente("LG-0001", 1000)).contains("1000").doesNotContain("Martín");
    }

    @Test
    void elAvisoDeOrdenNuevaIdentificaLaOrdenYElCaso() {
        assertThat(TextosNotificacion.nuevaOrden("LG-0001", 1000))
                .isEqualTo("Se registró la orden LG-0001 del paciente Código 1000.");
    }

    /** RN-11: el aviso al laboratorio se distingue del que recibe el odontólogo. */
    @Test
    void elAvisoDeOrdenUrgenteSeDistingueDelDeOrdenNueva() {
        assertThat(TextosNotificacion.ordenUrgente("LG-0001", 1000))
                .isNotEqualTo(TextosNotificacion.nuevaOrden("LG-0001", 1000))
                .contains("urgente");
    }
}
