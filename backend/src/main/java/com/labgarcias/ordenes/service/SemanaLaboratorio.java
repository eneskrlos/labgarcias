package com.labgarcias.ordenes.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * CU-02/CU-10: el corte de "entregadas esta semana", calculado en la zona horaria del laboratorio.
 *
 * `orden_historial_estado.fecha_hora` es TIMESTAMPTZ, así que el corte depende del huso: con la
 * semana partida en UTC, un trabajo entregado el domingo a las 21:00 en Montevideo caería en la
 * semana siguiente. La zona llega por properties y nunca va escrita en el código — un laboratorio
 * en otro huso cambia un valor de configuración, no una clase (D-16: una instalación por
 * laboratorio).
 *
 * La semana es la calendario, de lunes a domingo, y el rango es semiabierto `[desde, hasta)`: el
 * último instante del domingo entra y el primero del lunes siguiente no.
 */
@Component
public class SemanaLaboratorio {

    private final ZoneId zona;

    public SemanaLaboratorio(@Value("${app.laboratorio.zona-horaria}") String zonaHoraria) {
        this.zona = ZoneId.of(zonaHoraria);
    }

    /** Rango semiabierto de una semana: `desde` incluido, `hasta` excluido. */
    public record Rango(OffsetDateTime desde, OffsetDateTime hasta) {
    }

    public Rango semanaEnCurso() {
        return semanaDe(LocalDate.now(zona));
    }

    /**
     * La semana calendario que contiene a `referencia`. Se calcula una sola vez el lunes para que
     * los dos extremos salgan de la misma semana aunque la corrida cruce la medianoche.
     */
    Rango semanaDe(LocalDate referencia) {
        LocalDate lunes = referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Rango(
                lunes.atStartOfDay(zona).toOffsetDateTime(),
                lunes.plusWeeks(1).atStartOfDay(zona).toOffsetDateTime());
    }
}
