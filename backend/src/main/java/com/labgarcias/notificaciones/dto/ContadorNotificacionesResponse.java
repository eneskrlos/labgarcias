package com.labgarcias.notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §6.4: lo que consume la campana con `refetchInterval` de 60 s. Es un endpoint que se pide una
 * vez por minuto y por usuario, así que devuelve solo el número: nada de traer el listado para
 * contarlo del lado del cliente.
 */
public record ContadorNotificacionesResponse(

        @Schema(description = "Notificaciones propias sin leer", example = "3")
        long noLeidas

) {
}
