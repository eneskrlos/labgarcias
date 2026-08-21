package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** D-17/§3.1: formulario público de solicitud de acceso. Todos los campos son obligatorios. */
public record SolicitudAccesoRequest(

        @Schema(example = "Dr. Juan Pérez")
        @NotBlank(message = "El nombre completo es obligatorio.")
        @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
        String nombreCompleto,

        @Schema(example = "juan@mail.com")
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no tiene un formato válido.")
        @Size(max = 255, message = "El correo no puede superar los 255 caracteres.")
        String correo,

        @Schema(example = "Av. 18 de Julio 1234")
        @NotBlank(message = "La dirección es obligatoria.")
        @Size(max = 255, message = "La dirección no puede superar los 255 caracteres.")
        String direccion,

        @Schema(description = "Formato internacional E.164: + seguido del país y el número",
                example = "+59891234567")
        @NotBlank(message = "El teléfono es obligatorio.")
        @Pattern(regexp = "^\\+[1-9]\\d{7,14}$",
                message = "El teléfono debe estar en formato internacional, por ejemplo +59891234567.")
        String telefono

) {
}
