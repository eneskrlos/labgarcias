package com.labgarcias.seguridad.domain;

/**
 * D-17: estados de una solicitud de acceso (chk_solicitud_estado, V2).
 *
 * Quien la pasa a APROBADA es el alta de odontólogo de §3.1.b, no esta pantalla: aprobar una
 * solicitud y crear la cuenta son el mismo acto.
 */
public enum EstadoSolicitud {

    PENDIENTE,
    APROBADA,
    RECHAZADA
}
