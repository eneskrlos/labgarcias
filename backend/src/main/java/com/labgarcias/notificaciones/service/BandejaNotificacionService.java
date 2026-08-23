package com.labgarcias.notificaciones.service;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.labgarcias.notificaciones.domain.Notificacion;
import com.labgarcias.notificaciones.dto.ContadorNotificacionesResponse;
import com.labgarcias.notificaciones.dto.NotificacionResponse;
import com.labgarcias.notificaciones.repository.NotificacionRepository;
import com.labgarcias.shared.dto.PaginaResponse;
import com.labgarcias.shared.excepcion.RecursoNoEncontradoException;
import com.labgarcias.shared.util.ValidadorPaginacion;

/**
 * §6.4: la campana, del lado del usuario. Va aparte de NotificacionService porque son dos cosas
 * distintas sobre la misma tabla: aquel escribe el outbox cuando ocurre un evento; este contesta
 * lo que el destinatario pregunta. Juntarlos mezclaría la escritura automática con la lectura
 * interactiva en una clase que además pasaría las ~200 líneas (Agente.md 6.2).
 *
 * §6 criterio 3: el id del destinatario llega siempre desde el token, nunca por parámetro, y
 * viaja hasta la consulta. No hay forma de pedir las notificaciones de otro.
 */
@Service
public class BandejaNotificacionService {

    private final NotificacionRepository notificacionRepository;

    public BandejaNotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    /** §6.4: `leidas` es opcional; sin él vienen todas, leídas y no leídas. */
    @Transactional(readOnly = true)
    public PaginaResponse<NotificacionResponse> listarMias(Long destinatarioId, Boolean leidas, Pageable pageable) {
        ValidadorPaginacion.validarTamano(pageable.getPageSize());
        return PaginaResponse.de(buscar(destinatarioId, leidas, pageable).map(NotificacionResponse::de));
    }

    @Transactional(readOnly = true)
    public ContadorNotificacionesResponse contarNoLeidas(Long destinatarioId) {
        return new ContadorNotificacionesResponse(
                notificacionRepository.countByDestinatarioIdAndLeidaFalse(destinatarioId));
    }

    /** Marcar dos veces la misma notificación no mueve la fecha: la primera lectura es la que vale. */
    @Transactional
    public NotificacionResponse marcarLeida(Long id, Long destinatarioId) {
        Notificacion notificacion = notificacionRepository.findByIdAndDestinatarioId(id, destinatarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "NOTIFICACION_NO_ENCONTRADA", "No existe la notificación solicitada."));
        if (!notificacion.isLeida()) {
            notificacion.setLeida(true);
            notificacion.setFechaLectura(OffsetDateTime.now());
        }
        return NotificacionResponse.de(notificacion);
    }

    /**
     * Devuelve el contador ya actualizado —siempre cero— en vez de un mensaje: es exactamente lo
     * que la campana necesita para refrescarse, y le ahorra una segunda llamada.
     */
    @Transactional
    public ContadorNotificacionesResponse marcarTodasLeidas(Long destinatarioId) {
        notificacionRepository.marcarTodasLeidas(destinatarioId, OffsetDateTime.now());
        return contarNoLeidas(destinatarioId);
    }

    private Page<Notificacion> buscar(Long destinatarioId, Boolean leidas, Pageable pageable) {
        return leidas == null
                ? notificacionRepository.findByDestinatarioId(destinatarioId, pageable)
                : notificacionRepository.findByDestinatarioIdAndLeida(destinatarioId, leidas, pageable);
    }
}
