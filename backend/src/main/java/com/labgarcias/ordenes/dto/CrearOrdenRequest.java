package com.labgarcias.ordenes.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** CU-09: alta de una orden. El backend deriva iniciales, precios, estado inicial y fecha (RN-11, RN-18, RN-21, RN-22). */
public record CrearOrdenRequest(

        @Schema(description = "RN-22: uso interno, no se devuelve en ninguna respuesta", example = "Martín Pérez")
        @NotBlank(message = "El nombre del paciente es obligatorio.")
        String pacienteNombre,

        @Schema(example = "2026-08-06")
        @NotNull(message = "La fecha de ingreso es obligatoria.")
        LocalDate fechaIngreso,

        @Schema(example = "16")
        @NotNull(message = "El tipo de trabajo es obligatorio.")
        Integer tipoTrabajoId,

        @Schema(description = "RN-11: NORMAL o URGENTE", example = "NORMAL")
        @NotBlank(message = "El tipo de orden es obligatorio.")
        @Pattern(regexp = "NORMAL|URGENTE", message = "El tipo de orden debe ser NORMAL o URGENTE.")
        String tipoOrdenCodigo,

        @Schema(example = "Disyuntor superior, tornillo de expansión 7mm")
        String descripcion

) {
}
