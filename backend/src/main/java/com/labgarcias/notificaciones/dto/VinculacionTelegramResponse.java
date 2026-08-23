package com.labgarcias.notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * §6.5 paso 2: lo que el perfil necesita para mandar al usuario al bot. El token no se devuelve
 * aparte a propósito —ya viaja dentro del enlace— y no vuelve a mostrarse nunca más.
 */
public record VinculacionTelegramResponse(

        @Schema(description = "Enlace profundo que abre el bot con el token cargado",
                example = "https://t.me/labgarcias_bot?start=Zm9vYmFyMTIzNDU2Nzg5MGFiY2Rl")
        String enlace

) {
}
