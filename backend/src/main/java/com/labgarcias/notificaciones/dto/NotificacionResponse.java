package com.labgarcias.notificaciones.dto;

import java.time.OffsetDateTime;

import com.labgarcias.notificaciones.domain.Notificacion;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §6.4: una notificación tal como la ve su destinatario en la campana.
 *
 * No expone `notificacion_envio`: por qué canales salió y si alguno falló es información de
 * operación del laboratorio, no del usuario, y mostrarla solo lo confundiría (§6 criterio 2:
 * la notificación se ve igual aunque todos sus envíos hayan fallado).
 */
public record NotificacionResponse(

        @Schema(example = "12")
        Long id,

        @Schema(description = "Evento que la originó (§6.2)", example = "CAMBIO_ESTADO")
        String tipoEvento,

        @Schema(description = "RN-03/RN-22: identifica al paciente por su código, nunca por su nombre",
                example = "El trabajo del paciente Código 1000 pasó a la etapa de En producción.")
        String mensaje,

        @Schema(description = "Orden relacionada, para que la campana enlace a su detalle. "
                + "Nulo en los avisos que no son de una orden.", example = "7")
        Long ordenId,

        boolean leida,

        OffsetDateTime fechaCreacion,

        @Schema(description = "Nulo mientras no se marcó como leída")
        OffsetDateTime fechaLectura

) {
    public static NotificacionResponse de(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getTipoEvento().name(),
                notificacion.getMensaje(),
                notificacion.getOrdenId(),
                notificacion.isLeida(),
                notificacion.getFechaCreacion(),
                notificacion.getFechaLectura());
    }
}
