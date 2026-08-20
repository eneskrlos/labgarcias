package com.labgarcias.notificaciones.service;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.Canal;
import com.labgarcias.notificaciones.domain.EstadoEnvio;
import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.domain.NotificacionEnvio;
import com.labgarcias.notificaciones.domain.TipoEvento;
import com.labgarcias.notificaciones.repository.NotificacionEnvioRepository;
import com.labgarcias.notificaciones.repository.NotificacionRepository;
import com.labgarcias.seguridad.domain.Usuario;

import jakarta.persistence.EntityManager;

/**
 * PATRÓN: Transactional Outbox (lado de escritura)
 * PROBLEMA: si el aviso se enviara en el momento del evento, un SMTP caído dejaría al
 *           odontólogo sin enterarse de que su trabajo cambió de etapa, sin rastro de que
 *           hubo algo que avisar. Y si el envío se hiciera dentro de la transacción del
 *           negocio, un timeout del servidor de correo podría voltear el cambio de estado.
 * MOTIVADO POR: RN-05 (el cambio de estado notifica), §6.1 (outbox `notificacion` +
 *               `notificacion_envio`), §6 criterio 2 (correo caído: la notificación sobrevive).
 *
 * Registrar y entregar quedan separados: acá se anota la intención, y DespachadorNotificaciones
 * la resuelve después.
 */
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final NotificacionEnvioRepository envioRepository;
    private final SelectorCanales selectorCanales;
    private final EntityManager entityManager;

    public NotificacionService(NotificacionRepository notificacionRepository,
                               NotificacionEnvioRepository envioRepository,
                               SelectorCanales selectorCanales,
                               EntityManager entityManager) {
        this.notificacionRepository = notificacionRepository;
        this.envioRepository = envioRepository;
        this.selectorCanales = selectorCanales;
        this.entityManager = entityManager;
    }

    /**
     * §6.1 pasos 2 y 3: una notificación y un envío PENDIENTE por cada canal activo.
     *
     * REQUIRES_NEW porque quien llama es un listener AFTER_COMMIT: la transacción del negocio ya
     * cerró, y sin transacción propia el insert quedaría colgado de una que nunca va a confirmar.
     *
     * La notificación se guarda aunque no quede ningún canal activo: la campana de §6.4 lee esta
     * tabla, no la de envíos, así que el destinatario la ve igual. Es el mismo motivo por el que
     * el criterio 2 de §6 se cumple cuando falla el correo.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Notificacion registrar(Long destinatarioId, TipoEvento tipoEvento, String mensaje, Long ordenId) {
        Notificacion notificacion = new Notificacion();
        notificacion.setDestinatario(entityManager.getReference(Usuario.class, destinatarioId));
        notificacion.setTipoEvento(tipoEvento);
        notificacion.setMensaje(mensaje);
        notificacion.setOrdenId(ordenId);
        Notificacion persistida = notificacionRepository.save(notificacion);

        Set<Canal> canales = selectorCanales.canalesDe(tipoEvento, destinatarioId);
        envioRepository.saveAll(canales.stream().map(canal -> envioPendiente(persistida, canal)).toList());
        return persistida;
    }

    private NotificacionEnvio envioPendiente(Notificacion notificacion, Canal canal) {
        NotificacionEnvio envio = new NotificacionEnvio();
        envio.setNotificacion(notificacion);
        envio.setCanal(canal);
        envio.setEstadoEnvio(EstadoEnvio.PENDIENTE);
        return envio;
    }
}
