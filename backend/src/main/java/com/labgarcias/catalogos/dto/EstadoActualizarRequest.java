package com.labgarcias.catalogos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * CU-22/RN-04: solo nombre y descripcion son editables. No expone codigo,
 * orden_secuencia, es_terminal ni es_productivo — no hay forma de alterarlos
 * porque este DTO no los declara (el flujo lineal depende de ellos).
 */
public record EstadoActualizarRequest(

        @Schema(example = "En producción")
        @NotBlank(message = "El nombre es obligatorio.")
        String nombre,

        @Schema(example = "El trabajo está siendo confeccionado.")
        String descripcion

) {
}
