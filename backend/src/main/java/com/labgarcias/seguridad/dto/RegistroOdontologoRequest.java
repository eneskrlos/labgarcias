package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** RN-16: datos del auto-registro del odontólogo (CU-18). */
public record RegistroOdontologoRequest(

        @Schema(description = "Nombre completo del odontólogo", example = "Dr. Juan Pérez")
        @NotBlank(message = "El nombre completo es obligatorio.")
        String nombreCompleto,

        @Schema(description = "Correo electrónico; se usa para iniciar sesión y para verificar la cuenta", example = "juan@mail.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        String correo,

        @Schema(description = "Nombre de usuario, único en el sistema", example = "jperez")
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        String nombreUsuario,

        @Schema(description = "RN-15: mínimo 9 caracteres, con mayúscula, minúscula, número y carácter especial",
                example = "*38Op5)l6", format = "password")
        @NotBlank(message = "La contraseña es obligatoria.")
        String password,

        @Schema(description = "Dirección del odontólogo", example = "Av. 18 de Julio 1234")
        @NotBlank(message = "La dirección es obligatoria.")
        String direccion

) {
}
