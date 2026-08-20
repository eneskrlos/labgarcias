package com.labgarcias.notificaciones.dto;

import java.time.OffsetDateTime;

import com.labgarcias.notificaciones.domain.ConfiguracionNotificacion;

import io.swagger.v3.oas.annotations.media.Schema;

/** RN-19/CU-21: por qué canales recibe sus notificaciones el usuario autenticado. */
public record ConfiguracionNotificacionResponse(

        boolean canalAppActivo,

        boolean canalCorreoActivo,

        boolean canalTelegramActivo,

        @Schema(description = "P-18: solo lectura. WhatsApp es estructura hasta que se contrate un "
                + "proveedor, así que la configuración no lo puede encender.", example = "false")
        boolean canalWhatsappActivo,

        @Schema(description = "CU-21: obligatorio si canalTelegramActivo es true", example = "123456789")
        String telegramChatId,

        @Schema(description = "Nulo mientras el usuario no guardó ninguna configuración: rigen los "
                + "canales por defecto de §6.3 (app + correo + Telegram)")
        OffsetDateTime fechaActualizacion

) {
    public static ConfiguracionNotificacionResponse de(ConfiguracionNotificacion configuracion) {
        return new ConfiguracionNotificacionResponse(
                configuracion.isCanalAppActivo(),
                configuracion.isCanalCorreoActivo(),
                configuracion.isCanalTelegramActivo(),
                configuracion.isCanalWhatsappActivo(),
                configuracion.getTelegramChatId(),
                configuracion.getFechaActualizacion());
    }
}
