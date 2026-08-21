package com.labgarcias.seguridad.domain;

/**
 * D-17/§3.1: se publica al registrarse una solicitud de acceso.
 *
 * Lleva el nombre del solicitante porque es lo que necesita el texto del aviso, y el id para
 * que el administrador pueda ubicarla. No lleva correo, dirección ni teléfono: esos datos se
 * consultan en la pantalla de solicitudes y no tienen por qué quedar copiados en el mensaje
 * de una notificación, que además viaja por correo y Telegram.
 */
public record SolicitudAccesoEvent(Long solicitudId, String nombreCompleto) {
}
