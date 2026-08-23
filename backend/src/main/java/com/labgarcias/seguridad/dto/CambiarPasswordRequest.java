package com.labgarcias.seguridad.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** §3.1.b: cambio obligatorio en el primer ingreso. RN-15 sobre `passwordNueva` la valida el service. */
public record CambiarPasswordRequest(

        @Schema(description = "La contraseña temporal que llegó por correo", format = "password")
        @NotBlank(message = "La contraseña actual es obligatoria.")
        String passwordActual,

        @Schema(description = "RN-15: mínimo 9, con mayúsculas, minúsculas, números y especiales",
                format = "password")
        @NotBlank(message = "La contraseña nueva es obligatoria.")
        String passwordNueva

) {
}
