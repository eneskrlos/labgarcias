package com.labgarcias.ordenes.repository;

import java.time.LocalDate;

/**
 * §5.7: una fila de `v_ordenes_urgentes`.
 *
 * **La vista incluye `paciente_nombre` y esta proyección no lo declara a propósito** (RN-22,
 * `Agente.md` §8.2): el bloque de urgentes del dashboard es un listado, y ningún listado expone el
 * nombre del paciente. La consulta tampoco lo selecciona, así que el dato no llega ni siquiera a
 * la memoria del proceso. `DashboardRutasTest` lo vigila.
 */
public interface OrdenUrgenteProyeccion {

    Long getId();

    String getCodigo();

    String getOdontologo();

    String getEstado();

    LocalDate getFechaEstimadaEntrega();
}
