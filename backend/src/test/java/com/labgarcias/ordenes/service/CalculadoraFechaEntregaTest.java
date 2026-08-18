package com.labgarcias.ordenes.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/** RN-18: días hábiles excluyendo sábados y domingos. Sin feriados (S-05 sin resolver). */
class CalculadoraFechaEntregaTest {

    @Test
    void reproduceElEjemploDocumentadoEnSpec() {
        // spec.md §5.3: fechaIngreso 2026-08-06 (jueves) → fechaEstimadaEntrega 2026-08-17.
        LocalDate resultado = CalculadoraFechaEntrega.sumarDiasHabiles(LocalDate.of(2026, 8, 6), 7);

        assertThat(resultado).isEqualTo(LocalDate.of(2026, 8, 17));
    }

    @Test
    void criterio3UnaOrdenDeViernesCon7DiasHabilesCaeMartes() {
        LocalDate viernes = LocalDate.of(2026, 8, 7);
        assertThat(viernes.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);

        LocalDate resultado = CalculadoraFechaEntrega.sumarDiasHabiles(viernes, 7);

        assertThat(resultado.getDayOfWeek()).isEqualTo(DayOfWeek.TUESDAY);
        assertThat(resultado).isEqualTo(LocalDate.of(2026, 8, 18));
    }

    @Test
    void nuncaDevuelveUnSabadoNiUnDomingo() {
        LocalDate inicio = LocalDate.of(2026, 8, 3);
        for (int dias = 1; dias <= 30; dias++) {
            for (int corrimiento = 0; corrimiento < 7; corrimiento++) {
                LocalDate resultado = CalculadoraFechaEntrega.sumarDiasHabiles(inicio.plusDays(corrimiento), dias);
                assertThat(resultado.getDayOfWeek()).isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
            }
        }
    }

    @Test
    void saltaElFinDeSemanaCompleto() {
        // Jueves + 1 día hábil = viernes; viernes + 1 día hábil = lunes.
        assertThat(CalculadoraFechaEntrega.sumarDiasHabiles(LocalDate.of(2026, 8, 6), 1))
                .isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(CalculadoraFechaEntrega.sumarDiasHabiles(LocalDate.of(2026, 8, 7), 1))
                .isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void unaFechaDeIngresoEnSabadoArrancaElLunes() {
        LocalDate sabado = LocalDate.of(2026, 8, 8);
        assertThat(sabado.getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);

        assertThat(CalculadoraFechaEntrega.sumarDiasHabiles(sabado, 1)).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void laFechaEstimadaNuncaEsAnteriorALaDeIngreso() {
        // chk_orden_fechas exige fecha_estimada_entrega >= fecha_ingreso.
        LocalDate ingreso = LocalDate.of(2026, 8, 6);

        assertThat(CalculadoraFechaEntrega.sumarDiasHabiles(ingreso, 7)).isAfter(ingreso);
    }
}
