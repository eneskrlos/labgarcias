package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** CU-19 A1: correo al que se reenvía el enlace de verificación. */
public record ReenviarVerificacionRequest(

        @Schema(description = "Correo de la cuenta a verificar", example = "juan@mail.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo

) {
}
