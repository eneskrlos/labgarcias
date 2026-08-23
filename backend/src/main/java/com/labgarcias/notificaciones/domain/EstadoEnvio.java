package com.labgarcias.notificaciones.domain;

/** §6.1: ciclo de vida de un envío del outbox (chk_envio_estado). */
public enum EstadoEnvio {
    PENDIENTE,
    ENVIADO,
    FALLIDO
}
