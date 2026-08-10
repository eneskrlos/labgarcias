package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta genérica de confirmación, sin datos del usuario (ej.: CU-18 nunca devuelve el token ni el usuario). */
public record MensajeResponse(
        @Schema(description = "Mensaje informativo para mostrar al usuario",
                example = "Cuenta creada. Revisá tu correo para confirmarla.")
        String mensaje
) {
}
