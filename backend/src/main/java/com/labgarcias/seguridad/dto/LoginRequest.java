package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** CU-01: credenciales de inicio de sesión local. */
public record LoginRequest(

        @Schema(description = "Correo de la cuenta", example = "juan@mail.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo,

        @Schema(description = "Contraseña de la cuenta", example = "*38Op5)l6", format = "password")
        @NotBlank(message = "La contraseña es obligatoria.")
        String password

) {
}
