package com.labgarcias.ordenes.domain;

/**
 * RN-19/RN-11: se publica al crear una orden para que el módulo de notificaciones avise
 * al laboratorio. `notificaAdmin` viene de tipo_orden (no se deduce del código del tipo).
 * El envío efectivo lo implementa el bloque 4 (T-21); acá solo se publica.
 */
public record OrdenCreadaEvent(Long ordenId, String codigo, Integer pacienteCodigo, boolean notificaAdmin) {
}
