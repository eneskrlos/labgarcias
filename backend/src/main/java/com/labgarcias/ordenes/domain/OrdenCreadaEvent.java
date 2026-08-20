package com.labgarcias.ordenes.domain;

/**
 * §5.1 paso 10: se publica al registrar una orden.
 *
 * Trae dos destinatarios posibles, y por eso los dos datos:
 * - `odontologoId`, el dueño, que recibe NUEVA_ORDEN (D-19: la orden la carga el laboratorio,
 *   así que enterarse de que quedó registrada le importa a él).
 * - `notificaAdmin`, que viene de la tabla `tipo_orden` y decide si además hay ORDEN_URGENTE
 *   para el laboratorio. RN-11: sale de la tabla, nunca de comparar el código del tipo.
 */
public record OrdenCreadaEvent(Long ordenId,
                               String codigo,
                               Long odontologoId,
                               Integer pacienteCodigo,
                               boolean notificaAdmin) {
}
