package com.labgarcias.notificaciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CU-21: los canales se envían completos, no de a uno. Un PUT reemplaza la configuración entera,
 * así que las tres banderas son obligatorias: un `null` sería ambiguo entre "apagalo" y "no lo toques".
 *
 * `canalWhatsappActivo` **no está**: P-18 lo deja como estructura y encenderlo hoy solo produciría
 * envíos FALLIDO garantizados. La columna existe desde V2 y se sigue leyendo; cuando se contrate el
 * proveedor, se agrega acá sin tocar nada más.
 *
 * La regla de CU-21 (Telegram activo exige destino) se valida en el service, que es donde vive la
 * regla de negocio: Bean Validation no puede expresar la dependencia entre dos campos sin inventar
 * una anotación propia.
 */
public record ConfiguracionNotificacionRequest(

        @Schema(example = "true")
        @NotNull(message = "Indicá si el canal de la aplicación está activo.")
        Boolean canalAppActivo,

        @Schema(example = "true")
        @NotNull(message = "Indicá si el canal de correo está activo.")
        Boolean canalCorreoActivo,

        @Schema(example = "false")
        @NotNull(message = "Indicá si el canal de Telegram está activo.")
        Boolean canalTelegramActivo,

        @Schema(description = "CU-21: obligatorio si canalTelegramActivo es true", example = "123456789")
        @Size(max = 100, message = "El chat de Telegram no puede superar los 100 caracteres.")
        String telegramChatId

) {
}
