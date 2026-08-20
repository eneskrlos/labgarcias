package com.labgarcias.notificaciones.service;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.Notificacion;

/**
 * PATRÓN: Strategy + Adapter (puerto)
 * PROBLEMA: cada canal entrega el aviso de una forma completamente distinta —una fila que ya
 *           está en la base, un SMTP, una API HTTP de terceros— y qué canales se usan no lo
 *           decide el código sino la configuración de cada destinatario, en tiempo de ejecución.
 * MOTIVADO POR: RN-19 (canales configurables por usuario), RN-05 y D-20/D-21 (app, correo y
 *               Telegram para el cambio de estado), Agente.md 5.5 (puerto autorizado).
 *
 * El despachador no conoce ninguna implementación: pide la que soporta el canal del envío.
 * Agregar un canal es agregar un @Component, sin tocar el despachador.
 */
public interface CanalNotificacion {

    /** §6.3: qué canal atiende este adaptador. */
    boolean soporta(Canal canal);

    /**
     * Entrega la notificación. No toca el outbox: quien llama traduce el resultado a
     * ENVIADO o FALLIDO. Cualquier excepción se interpreta como fallo del envío, y su
     * mensaje queda en `detalle_error`.
     */
    void enviar(Notificacion notificacion);
}
