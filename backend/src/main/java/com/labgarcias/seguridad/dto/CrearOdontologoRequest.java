package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * D-18/§3.1.b: alta de un odontólogo por el administrador.
 *
 * **No lleva contraseña**: la genera el sistema (paso 2). Tampoco lleva rol ni estado: el rol es
 * siempre ODONTOLOGO y la cuenta nace ACTIVA.
 */
public record CrearOdontologoRequest(

        @Schema(example = "Dr. Juan Pérez")
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
        String nombreCompleto,

        @Schema(example = "juan@mail.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres.")
        String correo,

        @Schema(example = "jperez")
        @NotBlank(message = "El nombre de usuario es obligatorio.")
        @Size(max = 60, message = "El nombre de usuario no puede superar los 60 caracteres.")
        String nombreUsuario,

        @Schema(example = "Av. 18 de Julio 1234")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.")
        String direccion,

        @Schema(description = "Formato internacional E.164", example = "+59891234567")
        @Pattern(regexp = "^$|^\\+[1-9]\\d{7,14}$",
                message = "El teléfono debe estar en formato internacional, por ejemplo +59891234567.")
        String telefono,

        @Schema(description = "Opcional: si viene, esa solicitud pasa a APROBADA (§3.1.b)", example = "12")
        Long solicitudId

) {
}
