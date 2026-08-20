package com.labgarcias.notificaciones.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.ordenes.domain.EstadoOrdenCambiadoEvent;
import com.labgarcias.ordenes.domain.OrdenCreadaEvent;
import com.labgarcias.seguridad.domain.Usuario;
import com.labgarcias.seguridad.service.UsuarioService;

/**
 * PATRÓN: Observer
 * PROBLEMA: el módulo de órdenes no tiene por qué saber que existen las notificaciones, ni
 *           cargar con el tiempo ni con los fallos de un envío. Y al revés: una notificación
 *           de algo que terminó deshaciéndose sería una mentira para el odontólogo.
 * MOTIVADO POR: RN-05, Agente.md 5.6 (evento en vez de invocación directa), §5.5 criterio 4
 *               ("si la transacción falla, no se envía notificación").
 *
 * AFTER_COMMIT es la pieza que hace verdad ese criterio: hasta que la transacción de la orden no
 * confirma, este código no corre. Un rollback no deja ninguna fila en `notificacion`.
 */
@Component
public class OrdenNotificacionListener {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public OrdenNotificacionListener(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    /**
     * §5.1 paso 10 y §6.2: al dueño se le avisa que su orden quedó registrada; al laboratorio,
     * solo si `tipo_orden.notifica_admin` lo pide (RN-11, leído de la tabla y transportado por
     * el evento; acá no se compara ningún código de tipo).
     *
     * El aviso al admin *por orden nueva* que menciona el paso 10 no se emite: su destinatario
     * pasó a ser el odontólogo (D-19) y el paso lo deja previsto para cuando P-19 reabra la
     * creación por el odontólogo. Implementarlo hoy sería código sin uso.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarseUnaOrden(OrdenCreadaEvent evento) {
        notificacionService.registrar(
                evento.odontologoId(),
                TipoEvento.NUEVA_ORDEN,
                TextosNotificacion.nuevaOrden(evento.codigo(), evento.pacienteCodigo()),
                evento.ordenId());

        if (evento.notificaAdmin()) {
            avisarAlLaboratorio(evento);
        }
    }

    /** RN-05/CU-07/D-20: cada avance de etapa se le avisa al odontólogo dueño. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alCambiarDeEstado(EstadoOrdenCambiadoEvent evento) {
        notificacionService.registrar(
                evento.odontologoId(),
                TipoEvento.CAMBIO_ESTADO,
                TextosNotificacion.cambioEstado(evento.pacienteCodigo(), evento.estadoNombre()),
                evento.ordenId());
    }

    private void avisarAlLaboratorio(OrdenCreadaEvent evento) {
        String mensaje = TextosNotificacion.ordenUrgente(evento.codigo(), evento.pacienteCodigo());
        for (Usuario administrador : usuarioService.listarAdministradoresActivosParaNotificacion()) {
            notificacionService.registrar(administrador.getId(), TipoEvento.ORDEN_URGENTE, mensaje, evento.ordenId());
        }
    }
}
