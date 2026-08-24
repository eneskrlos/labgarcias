package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.labgarcias.ordenes.service.SemanaLaboratorio.Rango;

/**
 * CU-02/CU-10: el corte de "entregadas esta semana" se hace en el huso del laboratorio.
 *
 * El caso que motiva la clase es el domingo a la noche en Montevideo: en UTC ya es lunes, así que
 * un corte en UTC mandaría esa entrega a la semana siguiente.
 */
class SemanaLaboratorioTest {

    private static final String MONTEVIDEO = "America/Montevideo";

    /** Martes 18/08/2026: la semana arranca el lunes 17 y termina antes del lunes 24. */
    @Test
    void laSemanaVaDeLunesADomingoEnLaZonaDelLaboratorio() {
        Rango semana = new SemanaLaboratorio(MONTEVIDEO).semanaDe(LocalDate.of(2026, 8, 18));

        assertThat(semana.desde().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(semana.desde().toLocalTime()).isEqualTo("00:00");
        assertThat(semana.hasta().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 24));
        assertThat(semana.hasta().toLocalTime()).isEqualTo("00:00");
    }

    /** Un lunes es el primer día de su propia semana, no del anterior. */
    @Test
    void elLunesPerteneceASuPropiaSemana() {
        Rango semana = new SemanaLaboratorio(MONTEVIDEO).semanaDe(LocalDate.of(2026, 8, 17));

        assertThat(semana.desde().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    /** Y un domingo es el último, no el primero de la siguiente. */
    @Test
    void elDomingoEsElUltimoDiaDeLaSemana() {
        Rango semana = new SemanaLaboratorio(MONTEVIDEO).semanaDe(LocalDate.of(2026, 8, 23));

        assertThat(semana.desde().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 17));
        assertThat(semana.hasta().toLocalDate()).isEqualTo(LocalDate.of(2026, 8, 24));
    }

    /**
     * El motivo de que la zona no sea UTC: el domingo 23/08 a las 21:00 en Montevideo son las
     * 00:00 del lunes 24 en UTC. Con el corte en el huso del laboratorio esa entrega sigue
     * cayendo dentro de la semana; con el corte en UTC, no.
     */
    @Test
    void unaEntregaDelDomingoALaNocheEntraEnSuSemanaYNoEnLaSiguiente() {
        OffsetDateTime domingoALas21 = OffsetDateTime.of(2026, 8, 23, 21, 0, 0, 0, ZoneOffset.ofHours(-3));

        Rango deMontevideo = new SemanaLaboratorio(MONTEVIDEO).semanaDe(LocalDate.of(2026, 8, 23));
        Rango deUtc = new SemanaLaboratorio("UTC").semanaDe(LocalDate.of(2026, 8, 23));

        assertThat(domingoALas21).isBefore(deMontevideo.hasta());
        assertThat(domingoALas21).isAfterOrEqualTo(deMontevideo.desde());
        assertThat(domingoALas21)
                .as("con el corte en UTC la misma entrega se escapa a la semana siguiente")
                .isAfterOrEqualTo(deUtc.hasta());
    }

    /** El rango es semiabierto: el primer instante del lunes siguiente ya no pertenece. */
    @Test
    void elRangoEsSemiabiertoAsiQueNingunaEntregaSeCuentaDosVeces() {
        SemanaLaboratorio laboratorio = new SemanaLaboratorio(MONTEVIDEO);

        Rango estaSemana = laboratorio.semanaDe(LocalDate.of(2026, 8, 18));
        Rango laSiguiente = laboratorio.semanaDe(LocalDate.of(2026, 8, 25));

        assertThat(estaSemana.hasta()).isEqualTo(laSiguiente.desde());
    }
}
