package com.labgarcias.ordenes.domain;

/**
 * §5.5/RN-05: se publica al avanzar el estado de una orden para que el módulo de
 * notificaciones avise al odontólogo dueño (§6.2, tipo_evento CAMBIO_ESTADO).
 * Trae `pacienteCodigo` y `estadoNombre` porque son las dos piezas del texto que
 * documenta CU-07. El envío efectivo lo implementa T-21; acá solo se publica.
 *
 * La cancelación NO publica evento: no notifica (S-08 sin resolver).
 */
public record EstadoOrdenCambiadoEvent(Long ordenId,
                                       String codigo,
                                       Long odontologoId,
                                       Integer pacienteCodigo,
                                       String estadoNombre) {
}
