package com.labgarcias.licencia.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** CU-23: registro manual de un período de licencia por el SuperAdmin. */
public record RegistrarLicenciaRequest(

        @Schema(description = "Inicio del período de licencia", example = "2026-01-01")
        @NotNull(message = "La fecha de inicio es obligatoria.")
        LocalDate fechaInicio,

        @Schema(description = "Vencimiento del período de licencia", example = "2026-12-31")
        @NotNull(message = "La fecha de vencimiento es obligatoria.")
        LocalDate fechaVencimiento,

        @Schema(description = "Observación libre sobre la activación (opcional)", example = "Renovación anual acordada por correo")
        String observacion

) {
}
