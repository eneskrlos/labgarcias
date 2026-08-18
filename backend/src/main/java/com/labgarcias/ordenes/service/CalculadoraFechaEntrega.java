package com.labgarcias.ordenes.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * RN-18: la fecha estimada de entrega es la fecha de ingreso más los días hábiles
 * del tipo de trabajo, excluyendo sábados y domingos.
 * Los feriados NO se contemplan: S-05 sigue sin resolver (Agente.md 3.3).
 */
public final class CalculadoraFechaEntrega {

    private CalculadoraFechaEntrega() {
    }

    public static LocalDate sumarDiasHabiles(LocalDate fechaIngreso, int diasHabiles) {
        LocalDate fecha = fechaIngreso;
        int sumados = 0;
        while (sumados < diasHabiles) {
            fecha = fecha.plusDays(1);
            if (esDiaHabil(fecha)) {
                sumados++;
            }
        }
        return fecha;
    }

    private static boolean esDiaHabil(LocalDate fecha) {
        DayOfWeek dia = fecha.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }
}
