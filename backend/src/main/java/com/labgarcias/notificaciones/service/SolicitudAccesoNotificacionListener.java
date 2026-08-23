package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.seguridad.domain.SolicitudAccesoEvent;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;

/**
 * PATRÓN: Observer
 * PROBLEMA: el formulario público de solicitud no tiene por qué saber quiénes son los
 *           administradores del laboratorio ni por qué canales quieren enterarse, y un aviso
 *           de una solicitud que terminó no registrándose sería una mentira.
 * MOTIVADO POR: D-17 (§3.1: "Publica SolicitudAccesoEvent"), Agente.md 5.6 (evento en vez de
 *               invocación directa), §6.2 (destinatario y canales del evento).
 *
 * Como en el listener de órdenes, AFTER_COMMIT es lo que ata el aviso a que la solicitud haya
 * quedado guardada de verdad.
 */
@Component
public class SolicitudAccesoNotificacionListener {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public SolicitudAccesoNotificacionListener(NotificacionService notificacionService,
                                               UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    /**
     * §6.2: `SOLICITUD_ACCESO` va al Administrador, que son todas las cuentas de administración
     * activas, una notificación cada una (decisión de T-21). `ordenId` va nulo: este aviso no
     * es de una orden.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRecibirseUnaSolicitud(SolicitudAccesoEvent evento) {
        String mensaje = TextosNotificacion.solicitudAcceso(evento.nombreCompleto());
        for (Usuario administrador : usuarioService.listarAdministradoresActivosParaNotificacion()) {
            notificacionService.registrar(administrador.getId(), TipoEvento.SOLICITUD_ACCESO, mensaje, null);
        }
    }
}
