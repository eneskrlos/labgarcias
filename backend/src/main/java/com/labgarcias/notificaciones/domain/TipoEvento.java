package com.labgarcias.notificaciones.domain;

import java.util.Set;

/**
 * §6.2: eventos que generan notificación (chk_notificacion_evento, ampliado por V2).
 *
 * Cada constante declara por qué canales corresponde ese evento. Es la columna "Canales"
 * de la tabla de §6.2, copiada tal cual: acá vive el "por qué canales" del evento, mientras
 * que el "por qué canales quiere este usuario" (RN-19) vive en configuracion_notificacion.
 * SelectorCanales cruza las dos. Tenerlo como dato del enum evita un switch que habría que
 * tocar cada vez que la spec agregue un evento.
 */
public enum TipoEvento {

    /** Sin uso: era del auto-registro, eliminado por CR-01 (D-17/D-18). Se conserva por el CHECK. */
    CUENTA_CREADA(Set.of()),

    /** D-17: la solicitud de acceso avisa al administrador. */
    SOLICITUD_ACCESO(Set.of(Canal.APP, Canal.CORREO, Canal.TELEGRAM)),

    /** D-18/§3.1.b: alta de odontólogo. Solo correo: todavía no vinculó Telegram. */
    CREDENCIALES_CREADAS(Set.of(Canal.CORREO)),

    /** D-19/§5.1 paso 10: al odontólogo dueño, que su orden quedó registrada. */
    NUEVA_ORDEN(Set.of(Canal.CORREO, Canal.TELEGRAM)),

    /** RN-11: al administrador, cuando tipo_orden.notifica_admin lo pide. */
    ORDEN_URGENTE(Set.of(Canal.APP, Canal.CORREO, Canal.TELEGRAM)),

    /** RN-05/CU-07/D-20: al odontólogo dueño, cada avance de etapa. */
    CAMBIO_ESTADO(Set.of(Canal.APP, Canal.CORREO, Canal.TELEGRAM));

    private final Set<Canal> canales;

    TipoEvento(Set<Canal> canales) {
        this.canales = canales;
    }

    public Set<Canal> getCanales() {
        return canales;
    }
}
