package com.labgarcias.licencia.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record LicenciaResponse(

        @Schema(example = "1")
        Long id,

        @Schema(example = "2026-01-01")
        LocalDate fechaInicio,

        @Schema(example = "2026-12-31")
        LocalDate fechaVencimiento,

        @Schema(description = "ACTIVA o VENCIDA", example = "ACTIVA")
        String estado,

        @Schema(description = "Nombre del SuperAdmin que registró el período", example = "Dr. Super Admin")
        String activadaPorNombre,

        OffsetDateTime fechaRegistro,

        String observacion

) {
}
